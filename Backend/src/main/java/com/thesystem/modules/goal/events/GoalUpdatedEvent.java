package com.thesystem.modules.goal.events;

import com.thesystem.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record GoalUpdatedEvent(
        UUID goalId,
        UUID userId,
        String title,
        Instant occurredAt
) implements DomainEvent {
    public GoalUpdatedEvent(UUID goalId, UUID userId, String title) {
        this(goalId, userId, title, Instant.now());
    }
}
