package com.itq.document.service;

import com.itq.document.dto.*;
import com.itq.document.entity.*;
import com.itq.document.enums.DocumentAction;
import com.itq.document.enums.DocumentStatus;
import com.itq.document.exception.DocumentNotFoundException;
import com.itq.document.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final DocumentHistoryRepository historyRepository;
    private final ApprovalRegistryRepository registryRepository;
    private final NumberGenerator numberGenerator;
    private final DocumentMapper mapper;

    public DocumentService(DocumentRepository documentRepository,
                           DocumentHistoryRepository historyRepository,
                           ApprovalRegistryRepository registryRepository,
                           NumberGenerator numberGenerator,
                           DocumentMapper mapper) {
        this.documentRepository = documentRepository;
        this.historyRepository = historyRepository;
        this.registryRepository = registryRepository;
        this.numberGenerator = numberGenerator;
        this.mapper = mapper;
    }

    @Transactional
    public DocumentDto createDocument(CreateDocumentRequest req) {
        Document doc = new Document();
        doc.setNumber(numberGenerator.generate());
        doc.setAuthor(req.author());
        doc.setTitle(req.title());
        doc.setStatus(DocumentStatus.DRAFT);
        Document saved = documentRepository.save(doc);
        log.info("Created document id={} number={} author={}", saved.getId(), saved.getNumber(), saved.getAuthor());
        return mapper.toDto(saved, false);
    }

    @Transactional(readOnly = true)
    public DocumentDto getDocumentWithHistory(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));
        doc.getHistory().size();
        return mapper.toDto(doc, true);
    }

    @Transactional(readOnly = true)
    public Page<DocumentDto> getDocumentsByIds(List<Long> ids, Pageable pageable) {
        List<Document> all = documentRepository.findAllByIds(ids);
        Map<Long, Document> byId = new LinkedHashMap<>();
        for (Document d : all) byId.put(d.getId(), d);

        List<DocumentHistory> histories = historyRepository.findAllByDocumentIds(ids);
        Map<Long, List<DocumentHistory>> histByDocId = new HashMap<>();
        for (DocumentHistory h : histories) {
            histByDocId.computeIfAbsent(h.getDocument().getId(), k -> new ArrayList<>()).add(h);
        }
        for (Document d : all) {
            d.setHistory(histByDocId.getOrDefault(d.getId(), Collections.emptyList()));
        }

        List<DocumentDto> dtos = all.stream().map(d -> mapper.toDto(d, true)).toList();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), dtos.size());
        List<DocumentDto> page = (start >= dtos.size()) ? Collections.emptyList() : dtos.subList(start, end);
        return new PageImpl<>(page, pageable, dtos.size());
    }

    @Transactional
    public BatchOperationResult.ItemResult submitOne(Long id, String initiator, String comment) {
        Optional<Document> opt = documentRepository.findByIdWithLock(id);
        if (opt.isEmpty()) {
            return BatchOperationResult.ItemResult.notFound(id);
        }
        Document doc = opt.get();
        if (doc.getStatus() != DocumentStatus.DRAFT) {
            return BatchOperationResult.ItemResult.conflict(id,
                    "Document is in status %s, expected DRAFT".formatted(doc.getStatus()));
        }
        doc.setStatus(DocumentStatus.SUBMITTED);
        addHistory(doc, initiator, DocumentAction.SUBMIT, comment);
        documentRepository.save(doc);
        log.debug("Submitted document id={}", id);
        return BatchOperationResult.ItemResult.success(id);
    }

    @Transactional
    public BatchOperationResult.ItemResult approveOne(Long id, String initiator, String comment) {
        Optional<Document> opt = documentRepository.findByIdWithLock(id);
        if (opt.isEmpty()) {
            return BatchOperationResult.ItemResult.notFound(id);
        }
        Document doc = opt.get();
        if (doc.getStatus() != DocumentStatus.SUBMITTED) {
            return BatchOperationResult.ItemResult.conflict(id,
                    "Document is in status %s, expected SUBMITTED".formatted(doc.getStatus()));
        }
        doc.setStatus(DocumentStatus.APPROVED);
        addHistory(doc, initiator, DocumentAction.APPROVE, comment);
        documentRepository.save(doc);

        try {
            ApprovalRegistry registry = new ApprovalRegistry();
            registry.setDocumentId(doc.getId());
            registry.setDocumentNumber(doc.getNumber());
            registry.setApprovedBy(initiator);
            registry.setApprovedAt(Instant.now());
            registryRepository.saveAndFlush(registry);
        } catch (Exception e) {
            log.error("Failed to register approval for document id={}: {}", id, e.getMessage());
            throw new RegistryException("Failed to register approval for document " + id, e);
        }

        log.debug("Approved document id={}", id);
        return BatchOperationResult.ItemResult.success(id);
    }

    @Transactional(readOnly = true)
    public Page<DocumentDto> search(SearchRequest req, Pageable pageable) {
        return documentRepository.search(
                req.status(), req.author(), req.from(), req.to(), pageable
        ).map(d -> mapper.toDto(d, false));
    }

    private void addHistory(Document doc, String performedBy, DocumentAction action, String comment) {
        DocumentHistory h = new DocumentHistory();
        h.setDocument(doc);
        h.setPerformedBy(performedBy);
        h.setAction(action);
        h.setComment(comment);
        h.setPerformedAt(Instant.now());
        doc.getHistory().add(h);
    }

    public static class RegistryException extends RuntimeException {
        public RegistryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
