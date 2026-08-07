package com.thesystem.modules.xp.dto.xpaccount;

import java.time.Instant;
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
