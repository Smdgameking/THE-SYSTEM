package com.thesystem.modules.xp.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record XpAdjustedEvent(
        UUID userId,
        int amount,
        String reason,
        UUID adminId,
        Instant occurredAt
) implements DomainEvent {
    public XpAdjustedEvent(UUID userId, int amount, String reason, UUID adminId) {
        this(userId, amount, reason, adminId, Instant.now());
    }
}
