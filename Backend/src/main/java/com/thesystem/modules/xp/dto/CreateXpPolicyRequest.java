package com.thesystem.modules.xp.dto;

import com.thesystem.modules.xp.enums.PolicyType;

import java.util.Map;

public record CreateXpPolicyRequest(
        String code,
        String name,
        String description,
        PolicyType policyType,
        Integer baseXp,
        Double multiplier,
        Map<String, Object> conditions,
        Integer priority
) {
}
