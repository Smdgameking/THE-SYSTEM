package com.thesystem.modules.goal.dto;

import com.thesystem.modules.goal.enums.CompletionStrategy;
import com.thesystem.modules.goal.enums.GoalDifficulty;
import com.thesystem.modules.goal.enums.GoalPriority;
import com.thesystem.modules.goal.enums.GoalType;
import com.thesystem.modules.goal.enums.GoalVisibility;

import com.thesystem.modules.goal.enums.CompletionStrategy;
import com.thesystem.modules.goal.enums.GoalDifficulty;
import com.thesystem.modules.goal.enums.GoalPriority;
import com.thesystem.modules.goal.enums.GoalStatus;
import com.thesystem.modules.goal.enums.GoalVisibility;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GoalResponse(
        UUID id,
        UUID userId,
        String title,
        String description,
        String category,
        GoalPriority priority,
        GoalDifficulty difficulty,
        GoalStatus status,
        GoalVisibility visibility,
        Integer estimatedXp,
        Integer currentProgress,
        Double completionPercentage,
        Instant targetDate,
        Instant completedDate,
        Instant archivedDate,
        CompletionStrategy completionStrategy,
        List<String> tags,
        Map<String, Object> customMetadata,
        Instant createdAt,
        Instant updatedAt
) {
}
