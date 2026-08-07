package com.thesystem.modules.xp.dto;

import java.util.UUID;

public record LeaderboardEntryResponse(
        UUID userId,
        String username,
        Integer currentXp,
        Integer currentLevel,
        Integer rank
) {
}
