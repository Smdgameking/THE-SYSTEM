package com.thesystem.modules.ai.events;

import com.thesystem.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record AiInteractionCreatedEvent(
        UUID interactionId,
        UUID userId,
        String provider,
        Instant occurredAt
) implements DomainEvent {
    public AiInteractionCreatedEvent(UUID interactionId, UUID userId, String provider) {
        this(interactionId, userId, provider, Instant.now());
    }
}
