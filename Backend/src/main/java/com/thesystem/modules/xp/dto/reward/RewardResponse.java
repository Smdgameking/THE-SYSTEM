package com.thesystem.modules.xp.dto.reward;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record RewardResponse(
        UUID id,
        UUID userId,
        String rewardType,
        String sourceType,
        UUID sourceId,
        Integer xpAmount,
        UUID policyId,
        Double multiplierApplied,
        Integer baseAmount,
        Instant awardedAt,
        Map<String, Object> metadata
) {
}
