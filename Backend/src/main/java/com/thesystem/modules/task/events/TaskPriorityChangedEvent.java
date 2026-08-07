package com.thesystem.modules.task.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;

public record TaskPriorityChangedEvent(Long taskId, String oldPriority, String newPriority, Instant occurredAt) implements DomainEvent {
    public TaskPriorityChangedEvent(Long taskId, String oldPriority, String newPriority) {
        this(taskId, oldPriority, newPriority, Instant.now());
    }
}
