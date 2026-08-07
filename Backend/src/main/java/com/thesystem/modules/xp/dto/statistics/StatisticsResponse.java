package com.thesystem.modules.xp.dto.statistics;

import com.thesystem.modules.xp.enums.AchievementCategory;
import com.thesystem.modules.xp.enums.TransactionType;
import java.time.Instant;
import java.util.Map;

public record StatisticsResponse(
        Integer dailyXp,
        Integer weeklyXp,
        Integer monthlyXp,
        Integer lifetimeXp,
        Integer currentLevel,
        Double levelProgress,
        Integer tasksCompleted,
        Integer goalsCompleted,
        Integer achievementsUnlocked,
        Map<String, Long> xpBySource,
        Map<TransactionType, Long> xpByType,
        Map<AchievementCategory, Long> xpByAchievement,
        Instant generatedAt
) {
}
