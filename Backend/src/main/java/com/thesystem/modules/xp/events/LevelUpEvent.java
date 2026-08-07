package com.thesystem.modules.xp.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record LevelUpEvent(
        UUID userId,
        int oldLevel,
        int newLevel,
        int xpRequired,
        Instant occurredAt
) implements DomainEvent {
    public LevelUpEvent(UUID userId, int oldLevel, int newLevel, int xpRequired) {
        this(userId, oldLevel, newLevel, xpRequired, Instant.now());
    }
}
