package com.thesystem.modules.goal.dto;

import java.time.Instant;
import java.util.UUID;

public record CreateMilestoneRequest(
        String title,
        String description,
        Integer displayOrder
) {
}
