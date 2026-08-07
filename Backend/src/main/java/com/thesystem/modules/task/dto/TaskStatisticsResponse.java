package com.thesystem.modules.task.dto;

import com.thesystem.modules.task.enums.TaskPriority;
import com.thesystem.modules.task.enums.TaskStatus;

import java.time.Instant;
import java.util.Map;

public record TaskStatisticsResponse(
        Double completionRate,
        long totalTasks,
        long completedTasks,
        long failedTasks,
        long cancelledTasks,
        long archivedTasks,
        long overdueTasks,
        Double averageCompletionTime,
        long streakDays,
        long focusTimeToday,
        long focusTimeWeek,
        Map<TaskStatus, Long> tasksByStatus,
        Map<TaskPriority, Long> tasksByPriority,
        Map<String, Long> categoryBreakdown
) {
}
