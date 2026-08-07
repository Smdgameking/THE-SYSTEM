package com.thesystem.modules.task.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;

public record TimeEntryStoppedEvent(Long taskId, Long userId, java.time.LocalDateTime endTime, Integer durationMinutes, Instant occurredAt) implements DomainEvent {
    public TimeEntryStoppedEvent(Long taskId, Long userId, java.time.LocalDateTime endTime, Integer durationMinutes) {
        this(taskId, userId, endTime, durationMinutes, Instant.now());
    }
}
