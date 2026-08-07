package com.thesystem.modules.task.dto;

import com.thesystem.modules.task.enums.TaskExecutionType;
import com.thesystem.modules.task.enums.TaskPriority;
import com.thesystem.modules.task.enums.TaskStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TaskFilterRequest(
        List<TaskStatus> status,
        List<TaskPriority> priority,
        List<String> category,
        UUID goalId,
        UUID parentTaskId,
        Instant dueBefore,
        Instant dueAfter,
        List<String> tags,
        TaskExecutionType executionType,
        String search,
        String sortBy,
        String sortOrder,
        Integer page,
        Integer limit
) {
}
