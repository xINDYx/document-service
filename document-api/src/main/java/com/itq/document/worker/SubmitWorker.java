package com.itq.document.worker;

import com.itq.document.config.WorkerProperties;
import com.itq.document.dto.BatchOperationResult;
import com.itq.document.dto.BatchStatusRequest;
import com.itq.document.entity.Document;
import com.itq.document.enums.DocumentStatus;
import com.itq.document.repository.DocumentRepository;
import com.itq.document.service.BatchDocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SubmitWorker {

    private static final Logger log = LoggerFactory.getLogger(SubmitWorker.class);
    private static final String WORKER_INITIATOR = "submit-worker";

    private final DocumentRepository documentRepository;
    private final BatchDocumentService batchDocumentService;
    private final WorkerProperties properties;

    public SubmitWorker(DocumentRepository documentRepository,
                        BatchDocumentService batchDocumentService,
                        WorkerProperties properties) {
        this.documentRepository = documentRepository;
        this.batchDocumentService = batchDocumentService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${document.workers.submit-interval-ms:10000}")
    public void processSubmit() {
        int batchSize = properties.getBatchSize();
        List<Document> batch = documentRepository.findByStatusWithLimit(DocumentStatus.DRAFT, batchSize);

        if (batch.isEmpty()) {
            log.debug("SubmitWorker: no DRAFT documents found");
            return;
        }

        List<Long> ids = batch.stream().map(Document::getId).toList();
        log.info("SubmitWorker: processing {} DRAFT documents, ids=[{}..{}]",
                ids.size(), ids.getFirst(), ids.getLast());

        long start = System.currentTimeMillis();
        BatchOperationResult result = batchDocumentService.submitBatch(
                new BatchStatusRequest(ids, WORKER_INITIATOR, "Auto-submitted by worker")
        );
        long elapsed = System.currentTimeMillis() - start;

        long successCount = result.results().stream().filter(r -> "SUCCESS".equals(r.status())).count();
        long failCount = result.results().size() - successCount;
        log.info("SubmitWorker: done in {}ms — submitted={}, failed={}", elapsed, successCount, failCount);
    }
}
