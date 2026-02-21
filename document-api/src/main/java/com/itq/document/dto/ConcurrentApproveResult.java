package com.itq.document.dto;

import com.itq.document.enums.DocumentStatus;

public record ConcurrentApproveResult(
        int totalAttempts,
        int successCount,
        int conflictCount,
        int errorCount,
        DocumentStatus finalStatus
) {}
