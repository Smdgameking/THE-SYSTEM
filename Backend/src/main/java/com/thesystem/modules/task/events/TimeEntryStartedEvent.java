package com.thesystem.modules.task.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;

public record TimeEntryStartedEvent(Long taskId, Long userId, java.time.LocalDateTime startTime, Instant occurredAt) implements DomainEvent {
    public TimeEntryStartedEvent(Long taskId, Long userId, java.time.LocalDateTime startTime) {
        this(taskId, userId, startTime, Instant.now());
    }
}
