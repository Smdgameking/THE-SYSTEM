package com.thesystem.modules.goal.events;

import com.thesystem.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record GoalPausedEvent(
        UUID goalId,
        UUID userId,
        Instant occurredAt
) implements DomainEvent {
    public GoalPausedEvent(UUID goalId, UUID userId) {
        this(goalId, userId, Instant.now());
    }
}
