package com.thesystem.modules.task.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;
import java.util.List;

public record TaskCreatedEvent(Long taskId, Long userId, Long goalId, String title, Instant occurredAt) implements DomainEvent {
    public TaskCreatedEvent(Long taskId, Long userId, Long goalId, String title) {
        this(taskId, userId, goalId, title, Instant.now());
    }
}
