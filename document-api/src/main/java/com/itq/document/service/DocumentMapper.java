package com.itq.document.service;

import com.itq.document.dto.DocumentDto;
import com.itq.document.dto.HistoryDto;
import com.itq.document.entity.Document;
import com.itq.document.entity.DocumentHistory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class DocumentMapper {

    public DocumentDto toDto(Document doc, boolean includeHistory) {
        List<HistoryDto> history = includeHistory
                ? doc.getHistory().stream().map(this::toHistoryDto).toList()
                : Collections.emptyList();
        return new DocumentDto(
                doc.getId(),
                doc.getNumber(),
                doc.getAuthor(),
                doc.getTitle(),
                doc.getStatus(),
                doc.getCreatedAt(),
                doc.getUpdatedAt(),
                history
        );
    }

    public HistoryDto toHistoryDto(DocumentHistory h) {
        return new HistoryDto(
                h.getId(),
                h.getPerformedBy(),
                h.getPerformedAt(),
                h.getAction(),
                h.getComment()
        );
    }
}
