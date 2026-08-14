package com.thesystem.modules.task.dto;

import com.thesystem.modules.task.enums.TaskExecutionType;
import com.thesystem.modules.task.enums.TaskPriority;
import com.thesystem.modules.task.enums.TaskStatus;
import com.thesystem.modules.task.enums.TaskVisibility;
import com.thesystem.modules.task.enums.TaskDifficulty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record UpdateTaskRequest(
        @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
        String title,

        @Size(max = 5000, message = "Description must not exceed 5000 characters")
        String description,

        UUID goalId,
        UUID parentTaskId,
        TaskStatus status,
        TaskPriority priority,
        TaskDifficulty difficulty,

        @Size(max = 50, message = "Category must not exceed 50 characters")
        String category,

        TaskExecutionType executionType,

        @Positive(message = "Estimated duration must be a positive number")
        Integer estimatedDuration,

        @Positive(message = "Actual duration must be a positive number")
        Integer actualDuration,

        Instant startDate,
        Instant dueDate,
        Instant completedDate,
        Instant reminderDate,
        Boolean isRecurring,
        UUID recurringConfigId,

        @Size(max = 20, message = "Tags must not exceed 20")
        List<String> tags,

        List<Map<String, Object>> attachments,

        @Size(max = 5000, message = "Notes must not exceed 5000 characters")
        String notes,

        Map<String, Object> completionEvidence,
        Map<String, Object> executionState,
        Map<String, Object> customMetadata,
        TaskVisibility visibility
) {
}
