package com.thesystem.modules.task.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;

public record TaskActivatedEvent(Long taskId, Long userId, Instant occurredAt) implements DomainEvent {
    public TaskActivatedEvent(Long taskId, Long userId) {
        this(taskId, userId, Instant.now());
    }
}
