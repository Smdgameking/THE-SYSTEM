package com.thesystem.modules.task.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;
import java.util.List;

public record TaskDependencyCycleDetectedEvent(List<Long> taskIdsInCycle, Instant occurredAt) implements DomainEvent {
    public TaskDependencyCycleDetectedEvent(List<Long> taskIdsInCycle) {
        this(taskIdsInCycle, Instant.now());
    }
}
