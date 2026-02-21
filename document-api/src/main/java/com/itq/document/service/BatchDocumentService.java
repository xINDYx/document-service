package com.itq.document.service;

import com.itq.document.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BatchDocumentService {

    private static final Logger log = LoggerFactory.getLogger(BatchDocumentService.class);

    private final DocumentService documentService;

    public BatchDocumentService(DocumentService documentService) {
        this.documentService = documentService;
    }

    public BatchOperationResult submitBatch(BatchStatusRequest req) {
        List<BatchOperationResult.ItemResult> results = new ArrayList<>(req.ids().size());
        for (Long id : req.ids()) {
            try {
                BatchOperationResult.ItemResult result = documentService.submitOne(id, req.initiator(), req.comment());
                results.add(result);
            } catch (Exception e) {
                log.error("Unexpected error submitting document id={}: {}", id, e.getMessage());
                results.add(new BatchOperationResult.ItemResult(id, "ERROR", e.getMessage()));
            }
        }
        return new BatchOperationResult(results);
    }

    public BatchOperationResult approveBatch(BatchStatusRequest req) {
        List<BatchOperationResult.ItemResult> results = new ArrayList<>(req.ids().size());
        for (Long id : req.ids()) {
            try {
                BatchOperationResult.ItemResult result = documentService.approveOne(id, req.initiator(), req.comment());
                results.add(result);
            } catch (DocumentService.RegistryException e) {
                log.error("Registry error approving document id={}: {}", id, e.getMessage());
                results.add(BatchOperationResult.ItemResult.registryError(id));
            } catch (Exception e) {
                log.error("Unexpected error approving document id={}: {}", id, e.getMessage());
                results.add(new BatchOperationResult.ItemResult(id, "ERROR", e.getMessage()));
            }
        }
        return new BatchOperationResult(results);
    }
}
