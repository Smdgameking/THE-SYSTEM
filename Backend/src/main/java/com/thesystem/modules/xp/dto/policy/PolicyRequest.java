package com.thesystem.modules.xp.dto.policy;

import com.thesystem.modules.xp.enums.PolicyType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PolicyRequest(
        String code,
        String name,
        String description,
        PolicyType policyType,
        Integer baseXp,
        Double multiplier,
        Map<String, Object> conditions,
        Boolean isActive,
        Integer priority
) {
}
