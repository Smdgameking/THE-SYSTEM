package com.thesystem.modules.xp.dto;

import com.thesystem.modules.xp.enums.AchievementCategory;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record UserAchievementResponse(
        UUID id,
        UUID userId,
        UUID achievementId,
        String achievementCode,
        String achievementName,
        AchievementCategory category,
        Integer currentProgress,
        Integer targetProgress,
        Boolean isUnlocked,
        Instant unlockedAt,
        Map<String, Object> progressMetadata,
        Instant createdAt
) {
}
