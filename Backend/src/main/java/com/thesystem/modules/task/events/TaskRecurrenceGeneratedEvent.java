package com.thesystem.modules.task.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;

public record TaskRecurrenceGeneratedEvent(Long originalTaskId, Long newTaskId, Integer occurrenceNumber, Instant occurredAt) implements DomainEvent {
    public TaskRecurrenceGeneratedEvent(Long originalTaskId, Long newTaskId, Integer occurrenceNumber) {
        this(originalTaskId, newTaskId, occurrenceNumber, Instant.now());
    }
}
