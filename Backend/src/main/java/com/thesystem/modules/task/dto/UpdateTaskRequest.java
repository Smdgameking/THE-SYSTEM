package com.thesystem.modules.task.dto;

import com.thesystem.modules.task.enums.TaskExecutionType;
import com.thesystem.modules.task.enums.TaskPriority;
import com.thesystem.modules.task.enums.TaskStatus;
import com.thesystem.modules.task.enums.TaskVisibility;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record UpdateTaskRequest(
        String title,
        String description,
        UUID goalId,
        UUID parentTaskId,
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
        TaskVisibility visibility
) {
}
