package com.thesystem.modules.task.dto;

import com.thesystem.modules.task.enums.RecurrenceFrequency;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecurringConfigResponse(
        UUID id,
        UUID taskId,
        RecurrenceFrequency frequency,
        Integer intervalValue,
        String cronExpression,
        List<Integer> daysOfWeek,
        Integer dayOfMonth,
        Integer month,
        List<Instant> exceptionDates,
        Instant endDate,
        Integer maxOccurrences,
        Integer occurrenceCount,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy
) {
}
