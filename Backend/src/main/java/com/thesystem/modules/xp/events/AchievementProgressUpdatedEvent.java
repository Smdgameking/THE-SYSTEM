package com.thesystem.modules.xp.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record AchievementProgressUpdatedEvent(
        UUID userId,
        UUID achievementId,
        int currentProgress,
        int targetProgress,
        Instant occurredAt
) implements DomainEvent {
    public AchievementProgressUpdatedEvent(UUID userId, UUID achievementId, int currentProgress, int targetProgress) {
        this(userId, achievementId, currentProgress, targetProgress, Instant.now());
    }
}
