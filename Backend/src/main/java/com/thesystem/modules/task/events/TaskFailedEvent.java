package com.thesystem.modules.task.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;

public record TaskFailedEvent(Long taskId, Long userId, String reason, Instant occurredAt) implements DomainEvent {
    public TaskFailedEvent(Long taskId, Long userId, String reason) {
        this(taskId, userId, reason, Instant.now());
    }
}
