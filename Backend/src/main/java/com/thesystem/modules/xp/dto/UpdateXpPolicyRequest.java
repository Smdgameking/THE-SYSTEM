package com.thesystem.modules.xp.dto;

import java.util.Map;

public record UpdateXpPolicyRequest(
        String name,
        String description,
        Integer baseXp,
        Double multiplier,
        Map<String, Object> conditions,
        Boolean isActive,
        Integer priority
) {
}
