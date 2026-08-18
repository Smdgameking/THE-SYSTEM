package com.thesystem.modules.task.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record TaskUpdatedEvent(UUID taskId, UUID userId, UUID goalId, UUID previousGoalId, String title, Instant occurredAt) implements DomainEvent {
    public TaskUpdatedEvent(UUID taskId, UUID userId, UUID goalId, UUID previousGoalId, String title) {
        this(taskId, userId, goalId, previousGoalId, title, Instant.now());
    }
}
