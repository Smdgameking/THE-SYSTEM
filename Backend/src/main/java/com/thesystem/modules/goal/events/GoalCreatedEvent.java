package com.thesystem.modules.goal.events;

import com.thesystem.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record GoalCreatedEvent(
        UUID goalId,
        UUID userId,
        String title,
        String category,
        String type,
        Instant occurredAt
) implements DomainEvent {
    public GoalCreatedEvent(UUID goalId, UUID userId, String title, String category, String type) {
        this(goalId, userId, title, category, type, Instant.now());
    }
}
