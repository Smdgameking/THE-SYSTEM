package com.thesystem.modules.goal.events;

import com.thesystem.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record GoalProgressUpdatedEvent(
        UUID goalId,
        UUID userId,
        int oldProgress,
        int newProgress,
        double oldPercentage,
        double newPercentage,
        String source,
        Instant occurredAt
) implements DomainEvent {
    public GoalProgressUpdatedEvent(UUID goalId, UUID userId, int oldProgress, int newProgress, double oldPercentage, double newPercentage, String source) {
        this(goalId, userId, oldProgress, newProgress, oldPercentage, newPercentage, source, Instant.now());
    }
}
