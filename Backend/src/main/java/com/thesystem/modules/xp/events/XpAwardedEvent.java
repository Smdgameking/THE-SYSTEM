package com.thesystem.modules.xp.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;

public record XpAwardedEvent(
        int xpAmount,
        Long userId,
        String sourceType,
        Long sourceId,
        String transactionType,
        Instant occurredAt
) implements DomainEvent {
    public XpAwardedEvent(int xpAmount, Long userId, String sourceType, Long sourceId, String transactionType) {
        this(xpAmount, userId, sourceType, sourceId, transactionType, Instant.now());
    }
}
