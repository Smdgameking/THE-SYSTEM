package com.thesystem.modules.xp.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record XpRemovedEvent(
        int xpAmount,
        UUID userId,
        String reason,
        Instant occurredAt
) implements DomainEvent {
    public XpRemovedEvent(int xpAmount, UUID userId, String reason) {
        this(xpAmount, userId, reason, Instant.now());
    }
}
