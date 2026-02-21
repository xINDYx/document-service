package com.itq.document.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record BatchStatusRequest(
        @NotNull(message = "Ids must not be null")
        @NotEmpty(message = "Ids must not be empty")
        @Size(min = 1, max = 1000, message = "Ids count must be between 1 and 1000")
        List<@NotNull Long> ids,

        @NotBlank(message = "Initiator must not be blank")
        @Size(max = 255, message = "Initiator must not exceed 255 characters")
        String initiator,

        String comment
) {}
