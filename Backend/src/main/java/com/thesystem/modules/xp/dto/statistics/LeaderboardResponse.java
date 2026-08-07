package com.thesystem.modules.xp.dto.statistics;

import java.util.List;

public record LeaderboardResponse(
        List<LeaderboardEntry> entries,
        int totalPages,
        long totalElements
) {
}
