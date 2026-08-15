package com.thesystem.modules.memory.events;

import com.thesystem.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record MemoryDeletedEvent(
        UUID memoryId,
        UUID userId,
        Instant occurredAt
) implements DomainEvent {
    public MemoryDeletedEvent(UUID memoryId, UUID userId) {
        this(memoryId, userId, Instant.now());
    }
}
