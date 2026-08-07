package com.thesystem.modules.task.dto;

import com.thesystem.modules.task.enums.TaskTimeEntryType;

import java.time.Instant;
import java.util.UUID;

public record TimeEntryResponse(
        UUID id,
        UUID taskId,
        UUID userId,
        Instant startTime,
        Instant endTime,
        Integer durationMinutes,
        TaskTimeEntryType entryType,
        String notes,
        Instant createdAt,
        UUID createdBy,
        Instant deletedAt
) {
}
