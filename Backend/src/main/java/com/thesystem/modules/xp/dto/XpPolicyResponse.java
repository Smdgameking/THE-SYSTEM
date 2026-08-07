package com.thesystem.modules.xp.dto;

import com.thesystem.modules.xp.enums.PolicyType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record XpPolicyResponse(
        UUID id,
        String code,
        String name,
        String description,
        PolicyType policyType,
        Integer baseXp,
        Double multiplier,
        Map<String, Object> conditions,
        Boolean isActive,
        Integer priority,
        Instant createdAt,
        Instant updatedAt
) {
}
