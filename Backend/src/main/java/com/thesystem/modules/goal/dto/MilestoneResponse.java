package com.thesystem.modules.goal.dto;

import java.time.Instant;
import java.util.UUID;

public record MilestoneResponse(
        UUID id,
        UUID goalId,
        String title,
        String description,
        Integer displayOrder,
        Boolean isCompleted,
        Instant completedDate,
        Instant createdAt,
        Instant updatedAt
) {
}
