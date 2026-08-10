package com.thesystem.modules.task.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record TaskCompletedEvent(UUID taskId, UUID userId, UUID goalId, String title, String executionType, String difficulty, Instant occurredAt) implements DomainEvent {
    public TaskCompletedEvent(UUID taskId, UUID userId, UUID goalId, String title, String executionType, String difficulty) {
        this(taskId, userId, goalId, title, executionType, difficulty, Instant.now());
    }
}
