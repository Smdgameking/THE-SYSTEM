package com.thesystem.modules.task.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;

public record TaskDependencyRemovedEvent(Long taskId, Long dependsOnTaskId, Instant occurredAt) implements DomainEvent {
    public TaskDependencyRemovedEvent(Long taskId, Long dependsOnTaskId) {
        this(taskId, dependsOnTaskId, Instant.now());
    }
}
