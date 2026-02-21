package com.itq.document.dto;

import com.itq.document.enums.DocumentAction;
import java.time.Instant;

public record HistoryDto(
        Long id,
        String performedBy,
        Instant performedAt,
        DocumentAction action,
        String comment
) {}
