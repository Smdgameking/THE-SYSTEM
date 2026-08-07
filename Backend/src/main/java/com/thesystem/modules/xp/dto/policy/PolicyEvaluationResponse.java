package com.thesystem.modules.xp.dto.policy;

import java.util.Map;

public record PolicyEvaluationResponse(
        String policyCode,
        String policyName,
        boolean applicable,
        double multiplier,
        int baseXp,
        int calculatedXp,
        Map<String, Object> conditions
) {
}
