package com.thesystem.modules.xp.dto.achievement;

import com.thesystem.modules.xp.enums.AchievementCategory;
import com.thesystem.modules.xp.enums.RequirementType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AchievementResponse(
        UUID id,
        String code,
        String name,
        String description,
        AchievementCategory category,
        String iconUrl,
        RequirementType requirementType,
        Map<String, Object> requirementValue,
        Integer xpReward,
        Boolean isHidden,
        Boolean isRepeatable,
        Integer sortOrder,
        Instant createdAt
) {
}
