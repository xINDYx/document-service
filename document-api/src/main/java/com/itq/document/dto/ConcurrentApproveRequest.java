package com.itq.document.dto;

import jakarta.validation.constraints.*;

public record ConcurrentApproveRequest(
        @NotNull Long documentId,
        @NotBlank String initiator,
        @Min(1) @Max(50) int threads,
        @Min(1) @Max(200) int attempts
) {}
