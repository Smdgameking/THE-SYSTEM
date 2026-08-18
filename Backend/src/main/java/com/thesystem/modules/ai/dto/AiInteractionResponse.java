package com.thesystem.modules.ai.dto;

import com.thesystem.modules.ai.provider.AiContextItem;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AiInteractionResponse(
        UUID id,
        UUID userId,
        String message,
        String response,
        String provider,
        String model,
        List<AiContextItem> context,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        String finishReason,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy,
        UUID updatedBy,
        Instant deletedAt
) {
}
