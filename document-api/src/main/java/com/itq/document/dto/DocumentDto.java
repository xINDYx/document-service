package com.itq.document.dto;

import com.itq.document.enums.DocumentStatus;
import java.time.Instant;
import java.util.List;

public record DocumentDto(
        Long id,
        String number,
        String author,
        String title,
        DocumentStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<HistoryDto> history
) {}
