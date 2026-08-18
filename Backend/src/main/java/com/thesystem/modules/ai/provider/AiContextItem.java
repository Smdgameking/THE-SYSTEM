package com.thesystem.modules.ai.provider;

import java.util.List;

/**
 * A single deterministic context item derived from a user memory.
 */
public record AiContextItem(
        String title,
        String type,
        String importance,
        String source,
        String content
) {
}
