package com.itq.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDocumentRequest(
        @NotBlank(message = "Author must not be blank")
        @Size(max = 255, message = "Author must not exceed 255 characters")
        String author,

        @NotBlank(message = "Title must not be blank")
        @Size(max = 500, message = "Title must not exceed 500 characters")
        String title,

        @NotBlank(message = "Initiator must not be blank")
        @Size(max = 255, message = "Initiator must not exceed 255 characters")
        String initiator
) {}
