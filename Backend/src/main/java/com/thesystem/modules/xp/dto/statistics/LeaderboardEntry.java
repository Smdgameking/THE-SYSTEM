package com.thesystem.modules.xp.dto.statistics;

import java.util.UUID;

public record LeaderboardEntry(
        UUID userId,
        String username,
        Integer currentXp,
        Integer currentLevel,
        Integer rank
) {
}
