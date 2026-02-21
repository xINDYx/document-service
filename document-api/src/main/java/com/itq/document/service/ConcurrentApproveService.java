package com.itq.document.service;

import com.itq.document.dto.ConcurrentApproveRequest;
import com.itq.document.dto.ConcurrentApproveResult;
import com.itq.document.enums.DocumentStatus;
import com.itq.document.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ConcurrentApproveService {

    private static final Logger log = LoggerFactory.getLogger(ConcurrentApproveService.class);

    private final DocumentService documentService;
    private final DocumentRepository documentRepository;

    public ConcurrentApproveService(DocumentService documentService,
                                    DocumentRepository documentRepository) {
        this.documentService = documentService;
        this.documentRepository = documentRepository;
    }

    public ConcurrentApproveResult runConcurrentApprove(ConcurrentApproveRequest req) throws InterruptedException {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(req.threads());
        CountDownLatch latch = new CountDownLatch(1);

        List<Future<?>> futures = new ArrayList<>(req.attempts());
        for (int i = 0; i < req.attempts(); i++) {
            final int attempt = i;
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    var result = documentService.approveOne(
                            req.documentId(), req.initiator() + "-t" + attempt, null);
                    switch (result.status()) {
                        case "SUCCESS" -> successCount.incrementAndGet();
                        case "CONFLICT" -> conflictCount.incrementAndGet();
                        default -> errorCount.incrementAndGet();
                    }
                } catch (DocumentService.RegistryException e) {
                    errorCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errorCount.incrementAndGet();
                } catch (Exception e) {
                    log.warn("Concurrent attempt {} error: {}", attempt, e.getMessage());
                    conflictCount.incrementAndGet();
                }
            }));
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);

        DocumentStatus finalStatus = documentRepository.findById(req.documentId())
                .map(d -> d.getStatus())
                .orElse(null);

        log.info("Concurrent approve test: total={} success={} conflict={} error={} finalStatus={}",
                req.attempts(), successCount.get(), conflictCount.get(), errorCount.get(), finalStatus);

        return new ConcurrentApproveResult(
                req.attempts(),
                successCount.get(),
                conflictCount.get(),
                errorCount.get(),
                finalStatus
        );
    }
}
