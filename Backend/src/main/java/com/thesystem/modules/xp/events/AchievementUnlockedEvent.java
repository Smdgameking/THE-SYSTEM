package com.thesystem.modules.xp.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;

public record AchievementUnlockedEvent(
        Long userId,
        Long achievementId,
        String achievementCode,
        int xpReward,
        Instant occurredAt
) implements DomainEvent {
    public AchievementUnlockedEvent(Long userId, Long achievementId, String achievementCode, int xpReward) {
        this(userId, achievementId, achievementCode, xpReward, Instant.now());
    }
}
