package com.itq.document.controller;

import com.itq.document.dto.*;
import com.itq.document.enums.DocumentStatus;
import com.itq.document.service.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final BatchDocumentService batchService;
    private final ConcurrentApproveService concurrentApproveService;

    public DocumentController(DocumentService documentService,
                              BatchDocumentService batchService,
                              ConcurrentApproveService concurrentApproveService) {
        this.documentService = documentService;
        this.batchService = batchService;
        this.concurrentApproveService = concurrentApproveService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentDto createDocument(@Valid @RequestBody CreateDocumentRequest request) {
        return documentService.createDocument(request);
    }

    @GetMapping("/{id}")
    public DocumentDto getDocument(@PathVariable Long id) {
        return documentService.getDocumentWithHistory(id);
    }

    @PostMapping("/batch-get")
    public Page<DocumentDto> getDocumentsByIds(
            @RequestBody List<Long> ids,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return documentService.getDocumentsByIds(ids, pageable);
    }

    @PostMapping("/submit")
    public BatchOperationResult submitBatch(@Valid @RequestBody BatchStatusRequest request) {
        return batchService.submitBatch(request);
    }

    @PostMapping("/approve")
    public BatchOperationResult approveBatch(@Valid @RequestBody BatchStatusRequest request) {
        return batchService.approveBatch(request);
    }

    @GetMapping("/search")
    public Page<DocumentDto> search(
            @RequestParam(required = false) DocumentStatus status,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        SearchRequest req = new SearchRequest(status, author, from, to);
        return documentService.search(req, pageable);
    }

    @PostMapping("/concurrent-approve")
    public ConcurrentApproveResult concurrentApprove(
            @Valid @RequestBody ConcurrentApproveRequest request) throws InterruptedException {
        return concurrentApproveService.runConcurrentApprove(request);
    }
}
