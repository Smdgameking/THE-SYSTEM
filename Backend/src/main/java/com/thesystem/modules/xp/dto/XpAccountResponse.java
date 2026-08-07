package com.thesystem.modules.xp.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record XpAccountResponse(
        UUID id,
        UUID userId,
        Integer currentXp,
        Integer currentLevel,
        Integer totalXpEarned,
        Integer totalXpSpent,
        Integer lifetimeXp,
        Double levelProgress,
        Instant createdAt,
        Instant updatedAt
) {
}
