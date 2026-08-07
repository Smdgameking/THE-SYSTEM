package com.thesystem.modules.xp.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record AchievementUnlockedEvent(
        UUID userId,
        UUID achievementId,
        String achievementCode,
        int xpReward,
        Instant occurredAt
) implements DomainEvent {
    public AchievementUnlockedEvent(UUID userId, UUID achievementId, String achievementCode, int xpReward) {
        this(userId, achievementId, achievementCode, xpReward, Instant.now());
    }
}
