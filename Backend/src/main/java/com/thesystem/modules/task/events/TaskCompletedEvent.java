package com.thesystem.modules.task.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;

public record TaskCompletedEvent(Long taskId, Long userId, Long goalId, String title, String executionType, String difficulty, Instant occurredAt) implements DomainEvent {
    public TaskCompletedEvent(Long taskId, Long userId, Long goalId, String title, String executionType, String difficulty) {
        this(taskId, userId, goalId, title, executionType, difficulty, Instant.now());
    }
}
