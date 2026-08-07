package com.thesystem.modules.xp.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record XpAwardedEvent(
        int xpAmount,
        UUID userId,
        String sourceType,
        UUID sourceId,
        String transactionType,
        Instant occurredAt
) implements DomainEvent {
    public XpAwardedEvent(int xpAmount, UUID userId, String sourceType, UUID sourceId, String transactionType) {
        this(xpAmount, userId, sourceType, sourceId, transactionType, Instant.now());
    }
}
