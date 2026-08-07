package com.thesystem.modules.xp.dto.level;

public record ProgressResponse(
        int currentLevel,
        int currentXp,
        int xpRequiredForLevel,
        int xpProgress,
        int xpRemaining,
        double progressPercentage
) {
}
