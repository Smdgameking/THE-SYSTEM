package com.thesystem.modules.xp.dto.reward;

import java.util.Map;
import java.util.UUID;

public record RewardCalculationRequest(
        UUID userId,
        String rewardType,
        String sourceType,
        UUID sourceId,
        Integer baseXp,
        Map<String, Object> context
) {
}
