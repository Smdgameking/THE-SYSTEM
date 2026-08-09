package com.thesystem.modules.goal.events;

import com.thesystem.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record GoalCompletedEvent(
        UUID goalId,
        UUID userId,
        int estimatedXp,
        String difficulty,
        Instant occurredAt
) implements DomainEvent {
    public GoalCompletedEvent(UUID goalId, UUID userId, int estimatedXp, String difficulty) {
        this(goalId, userId, estimatedXp, difficulty, Instant.now());
    }
}
