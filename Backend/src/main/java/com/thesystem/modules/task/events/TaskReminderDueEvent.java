package com.thesystem.modules.task.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;

public record TaskReminderDueEvent(Long taskId, Long userId, java.time.LocalDateTime dueDate, Instant occurredAt) implements DomainEvent {
    public TaskReminderDueEvent(Long taskId, Long userId, java.time.LocalDateTime dueDate) {
        this(taskId, userId, dueDate, Instant.now());
    }
}
