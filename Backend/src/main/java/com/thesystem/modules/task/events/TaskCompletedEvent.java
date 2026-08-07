package com.thesystem.modules.task.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;

public record TaskCompletedEvent(Long taskId, Long userId, Long goalId, String title, String executionType, Instant occurredAt) implements DomainEvent {
    public TaskCompletedEvent(Long taskId, Long userId, Long goalId, String title, String executionType) {
        this(taskId, userId, goalId, title, executionType, Instant.now());
    }
}
