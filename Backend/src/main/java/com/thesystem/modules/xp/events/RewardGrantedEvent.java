package com.thesystem.modules.xp.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record RewardGrantedEvent(
        UUID userId,
        String rewardType,
        String sourceType,
        UUID sourceId,
        int xpAmount,
        Instant occurredAt
) implements DomainEvent {
    public RewardGrantedEvent(UUID userId, String rewardType, String sourceType, UUID sourceId, int xpAmount) {
        this(userId, rewardType, sourceType, sourceId, xpAmount, Instant.now());
    }
}
