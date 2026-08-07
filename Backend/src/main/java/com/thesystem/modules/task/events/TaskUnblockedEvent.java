package com.thesystem.modules.task.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;
import java.util.List;

public record TaskUnblockedEvent(Long taskId, Long userId, List<Long> resolvedDependencyIds, Instant occurredAt) implements DomainEvent {
    public TaskUnblockedEvent(Long taskId, Long userId, List<Long> resolvedDependencyIds) {
        this(taskId, userId, resolvedDependencyIds, Instant.now());
    }
}
