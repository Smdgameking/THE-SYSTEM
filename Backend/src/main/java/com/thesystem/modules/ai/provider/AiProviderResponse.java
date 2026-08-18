package com.thesystem.modules.ai.provider;

/**
 * Response returned by an {@link AiProvider} for a single interaction.
 */
public record AiProviderResponse(
        String content,
        String model,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        String finishReason
) {
}
