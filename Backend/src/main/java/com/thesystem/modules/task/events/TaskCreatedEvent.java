package com.thesystem.modules.task.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record TaskCreatedEvent(UUID taskId, UUID userId, UUID goalId, String title, Instant occurredAt) implements DomainEvent {
    public TaskCreatedEvent(UUID taskId, UUID userId, UUID goalId, String title) {
        this(taskId, userId, goalId, title, Instant.now());
    }
}
