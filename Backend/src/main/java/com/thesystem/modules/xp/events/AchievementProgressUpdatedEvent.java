package com.thesystem.modules.xp.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;

public record AchievementProgressUpdatedEvent(
        Long userId,
        Long achievementId,
        int currentProgress,
        int targetProgress,
        Instant occurredAt
) implements DomainEvent {
    public AchievementProgressUpdatedEvent(Long userId, Long achievementId, int currentProgress, int targetProgress) {
        this(userId, achievementId, currentProgress, targetProgress, Instant.now());
    }
}
