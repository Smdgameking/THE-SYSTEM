package com.thesystem.modules.goal.dto;

import com.thesystem.modules.goal.enums.CompletionStrategy;
import com.thesystem.modules.goal.enums.GoalDifficulty;
import com.thesystem.modules.goal.enums.GoalPriority;
import com.thesystem.modules.goal.enums.GoalType;
import com.thesystem.modules.goal.enums.GoalVisibility;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CreateGoalRequest(
        String title,
        String description,
        String category,
        GoalPriority priority,
        GoalDifficulty difficulty,
        GoalType type,
        GoalVisibility visibility,
        Integer estimatedXp,
        Instant targetDate,
        CompletionStrategy completionStrategy,
        List<String> tags,
        Map<String, Object> customMetadata
) {
}
