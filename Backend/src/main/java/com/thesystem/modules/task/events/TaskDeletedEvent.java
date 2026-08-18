package com.thesystem.modules.task.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record TaskDeletedEvent(UUID taskId, UUID userId, UUID goalId, Instant occurredAt) implements DomainEvent {
    public TaskDeletedEvent(UUID taskId, UUID userId, UUID goalId) {
        this(taskId, userId, goalId, Instant.now());
    }
}
