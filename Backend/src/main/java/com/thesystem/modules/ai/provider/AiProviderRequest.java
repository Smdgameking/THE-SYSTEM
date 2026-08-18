package com.thesystem.modules.ai.provider;

import java.util.List;

/**
 * Payload handed to an {@link AiProvider} for a single interaction.
 */
public record AiProviderRequest(
        String message,
        List<AiContextItem> context
) {
}
