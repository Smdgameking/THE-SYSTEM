package com.thesystem.modules.xp.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;

public record XpAdjustedEvent(
        Long userId,
        int amount,
        String reason,
        Long adminId,
        Instant occurredAt
) implements DomainEvent {
    public XpAdjustedEvent(Long userId, int amount, String reason, Long adminId) {
        this(userId, amount, reason, adminId, Instant.now());
    }
}
