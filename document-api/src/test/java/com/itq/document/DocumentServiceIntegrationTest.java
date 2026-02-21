package com.itq.document;

import com.itq.document.dto.*;
import com.itq.document.enums.DocumentStatus;
import com.itq.document.repository.ApprovalRegistryRepository;
import com.itq.document.repository.DocumentHistoryRepository;
import com.itq.document.repository.DocumentRepository;
import com.itq.document.service.BatchDocumentService;
import com.itq.document.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DocumentServiceIntegrationTest {

    @Autowired private DocumentService documentService;
    @Autowired private BatchDocumentService batchService;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private DocumentHistoryRepository historyRepository;
    @Autowired private ApprovalRegistryRepository registryRepository;

    @BeforeEach
    void cleanUp() {
        registryRepository.deleteAll();
        historyRepository.deleteAll();
        documentRepository.deleteAll();
    }

    // -------------------------------------------------------------------------
    // 1. Happy-path: single document full lifecycle
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Happy-path: create -> submit -> approve -> registry")
    void happyPath_singleDocument() {
        DocumentDto created = documentService.createDocument(
                new CreateDocumentRequest("Alice", "Test Doc", "alice"));
        assertThat(created.id()).isNotNull();
        assertThat(created.status()).isEqualTo(DocumentStatus.DRAFT);
        assertThat(created.number()).startsWith("DOC-");

        var submitResult = documentService.submitOne(created.id(), "alice", null);
        assertThat(submitResult.status()).isEqualTo("SUCCESS");

        DocumentDto afterSubmit = documentService.getDocumentWithHistory(created.id());
        assertThat(afterSubmit.status()).isEqualTo(DocumentStatus.SUBMITTED);
        assertThat(afterSubmit.history()).hasSize(1);
        assertThat(afterSubmit.history().getFirst().action().name()).isEqualTo("SUBMIT");

        var approveResult = documentService.approveOne(created.id(), "bob", "Looks good");
        assertThat(approveResult.status()).isEqualTo("SUCCESS");

        DocumentDto afterApprove = documentService.getDocumentWithHistory(created.id());
        assertThat(afterApprove.status()).isEqualTo(DocumentStatus.APPROVED);
        assertThat(afterApprove.history()).hasSize(2);

        assertThat(registryRepository.existsByDocumentId(created.id())).isTrue();
    }

    // -------------------------------------------------------------------------
    // 2. Batch submit with partial results
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Batch submit: mixed statuses — partial success")
    void batchSubmit_partialSuccess() {
        Long id1 = documentService.createDocument(new CreateDocumentRequest("Bob", "Doc1", "bob")).id();
        Long id2 = documentService.createDocument(new CreateDocumentRequest("Bob", "Doc2", "bob")).id();
        Long id3 = documentService.createDocument(new CreateDocumentRequest("Bob", "Doc3", "bob")).id();

        documentService.submitOne(id2, "bob", null);

        Long nonExistent = 999_999L;
        BatchOperationResult result = batchService.submitBatch(
                new BatchStatusRequest(List.of(id1, id2, id3, nonExistent), "bob", null));

        assertThat(result.results()).hasSize(4);
        var byId = result.results().stream()
                .collect(Collectors.toMap(BatchOperationResult.ItemResult::id, r -> r));

        assertThat(byId.get(id1).status()).isEqualTo("SUCCESS");
        assertThat(byId.get(id2).status()).isEqualTo("CONFLICT");
        assertThat(byId.get(id3).status()).isEqualTo("SUCCESS");
        assertThat(byId.get(nonExistent).status()).isEqualTo("NOT_FOUND");

        assertThat(documentRepository.findById(id1).get().getStatus()).isEqualTo(DocumentStatus.SUBMITTED);
        assertThat(documentRepository.findById(id3).get().getStatus()).isEqualTo(DocumentStatus.SUBMITTED);
    }

    // -------------------------------------------------------------------------
    // 3. Batch approve with partial results
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Batch approve: partial results — DRAFT conflict, SUBMITTED success, not found")
    void batchApprove_partialResults() {
        Long draft = documentService.createDocument(new CreateDocumentRequest("Carol", "D1", "carol")).id();
        Long submitted = documentService.createDocument(new CreateDocumentRequest("Carol", "D2", "carol")).id();
        documentService.submitOne(submitted, "carol", null);

        Long notFound = 888_888L;

        BatchOperationResult result = batchService.approveBatch(
                new BatchStatusRequest(List.of(draft, submitted, notFound), "carol", null));

        assertThat(result.results()).hasSize(3);
        var byId = result.results().stream()
                .collect(Collectors.toMap(BatchOperationResult.ItemResult::id, r -> r));

        assertThat(byId.get(draft).status()).isEqualTo("CONFLICT");
        assertThat(byId.get(submitted).status()).isEqualTo("SUCCESS");
        assertThat(byId.get(notFound).status()).isEqualTo("NOT_FOUND");

        assertThat(registryRepository.existsByDocumentId(submitted)).isTrue();
        assertThat(registryRepository.existsByDocumentId(draft)).isFalse();

        DocumentDto approvedDoc = documentService.getDocumentWithHistory(submitted);
        assertThat(approvedDoc.history()).hasSize(2);
        assertThat(approvedDoc.history().getLast().action().name()).isEqualTo("APPROVE");
    }

    // -------------------------------------------------------------------------
    // 4. Rollback approve: second attempt must return CONFLICT, registry unchanged
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Approve rollback: second approve returns CONFLICT, registry has exactly 1 entry")
    void approveRollback_secondAttemptIsConflict() {
        Long id = documentService.createDocument(new CreateDocumentRequest("Dave", "RollbackDoc", "dave")).id();
        documentService.submitOne(id, "dave", null);

        var first = documentService.approveOne(id, "dave", null);
        assertThat(first.status()).isEqualTo("SUCCESS");

        var second = documentService.approveOne(id, "dave", null);
        assertThat(second.status()).isEqualTo("CONFLICT");

        long count = registryRepository.findAll().stream()
                .filter(r -> r.getDocumentId().equals(id))
                .count();
        assertThat(count).isEqualTo(1);

        assertThat(documentRepository.findById(id).get().getStatus()).isEqualTo(DocumentStatus.APPROVED);
    }
}
