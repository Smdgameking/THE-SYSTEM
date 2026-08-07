package com.thesystem.modules.task.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;

public record TaskSubtaskCompletedEvent(Long parentTaskId, Long subtaskId, Instant occurredAt) implements DomainEvent {
    public TaskSubtaskCompletedEvent(Long parentTaskId, Long subtaskId) {
        this(parentTaskId, subtaskId, Instant.now());
    }
}
