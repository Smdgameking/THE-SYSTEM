package com.thesystem.modules.task.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;
import java.util.List;

public record TaskBlockedEvent(Long taskId, Long userId, List<Long> blockingDependencyIds, Instant occurredAt) implements DomainEvent {
    public TaskBlockedEvent(Long taskId, Long userId, List<Long> blockingDependencyIds) {
        this(taskId, userId, blockingDependencyIds, Instant.now());
    }
}
