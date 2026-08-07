package com.thesystem.modules.xp.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;

public record RewardGrantedEvent(
        Long userId,
        String rewardType,
        String sourceType,
        Long sourceId,
        int xpAmount,
        Instant occurredAt
) implements DomainEvent {
    public RewardGrantedEvent(Long userId, String rewardType, String sourceType, Long sourceId, int xpAmount) {
        this(userId, rewardType, sourceType, sourceId, xpAmount, Instant.now());
    }
}
