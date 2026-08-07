package com.thesystem.modules.goal.dto;

import com.thesystem.modules.goal.enums.GoalStatus;
import com.thesystem.modules.goal.enums.GoalPriority;

import java.time.Instant;
import java.util.Map;

public record GoalStatisticsResponse(
        long totalGoals,
        long activeGoals,
        long completedGoals,
        long failedGoals,
        long archivedGoals,
        double averageCompletionPercentage,
        Map<GoalStatus, Long> goalsByStatus,
        Map<GoalPriority, Long> goalsByPriority
) {
}
