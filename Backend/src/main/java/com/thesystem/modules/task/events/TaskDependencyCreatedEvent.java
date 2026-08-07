package com.thesystem.modules.task.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;

public record TaskDependencyCreatedEvent(Long taskId, Long dependsOnTaskId, String dependencyType, Instant occurredAt) implements DomainEvent {
    public TaskDependencyCreatedEvent(Long taskId, Long dependsOnTaskId, String dependencyType) {
        this(taskId, dependsOnTaskId, dependencyType, Instant.now());
    }
}
