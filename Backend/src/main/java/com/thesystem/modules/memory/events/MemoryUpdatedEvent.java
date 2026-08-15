package com.thesystem.modules.memory.events;

import com.thesystem.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record MemoryUpdatedEvent(
        UUID memoryId,
        UUID userId,
        String type,
        String title,
        Instant occurredAt
) implements DomainEvent {
    public MemoryUpdatedEvent(UUID memoryId, UUID userId, String type, String title) {
        this(memoryId, userId, type, title, Instant.now());
    }
}
