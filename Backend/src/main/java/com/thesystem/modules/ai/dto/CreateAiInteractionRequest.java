package com.thesystem.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAiInteractionRequest(
        @NotBlank(message = "Message is required")
        @Size(max = 4000, message = "Message must not exceed 4000 characters")
        String message,

        Boolean includeMemoryContext
) {
}
