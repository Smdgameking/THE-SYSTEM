package com.thesystem.modules.xp.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;

public record XpRemovedEvent(
        int xpAmount,
        Long userId,
        String reason,
        Instant occurredAt
) implements DomainEvent {
    public XpRemovedEvent(int xpAmount, Long userId, String reason) {
        this(xpAmount, userId, reason, Instant.now());
    }
}
