package com.itq.document.dto;

import com.itq.document.enums.DocumentStatus;
import java.time.Instant;

public record SearchRequest(
        DocumentStatus status,
        String author,
        Instant from,
        Instant to
) {}
