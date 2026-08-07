package com.thesystem.modules.task.dto;

import com.thesystem.modules.task.enums.DependencyStatus;
import com.thesystem.modules.task.enums.DependencyType;
import com.thesystem.modules.task.enums.TaskExecutionType;
import com.thesystem.modules.task.enums.TaskPriority;
import com.thesystem.modules.task.enums.TaskStatus;
import com.thesystem.modules.task.enums.TaskVisibility;
import com.thesystem.modules.task.enums.TaskTimeEntryType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID userId,
        UUID goalId,
        UUID parentTaskId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        String category,
        TaskExecutionType executionType,
        Integer estimatedDuration,
        Integer actualDuration,
        Instant startDate,
        Instant dueDate,
        Instant completedDate,
        Instant reminderDate,
        Boolean isRecurring,
        UUID recurringConfigId,
        List<String> tags,
        List<Map<String, Object>> attachments,
        String notes,
        Map<String, Object> completionEvidence,
        Map<String, Object> executionState,
        Map<String, Object> customMetadata,
        TaskVisibility visibility,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy,
        UUID updatedBy,
        Instant deletedAt
) {
}
