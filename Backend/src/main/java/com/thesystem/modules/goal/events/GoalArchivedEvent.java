package com.thesystem.modules.goal.events;

import com.thesystem.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record GoalArchivedEvent(
        UUID goalId,
        UUID userId,
        Instant occurredAt
) implements DomainEvent {
    public GoalArchivedEvent(UUID goalId, UUID userId) {
        this(goalId, userId, Instant.now());
    }
}
