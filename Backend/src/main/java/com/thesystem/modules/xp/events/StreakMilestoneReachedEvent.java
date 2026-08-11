package com.thesystem.modules.xp.events;

import com.thesystem.shared.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record StreakMilestoneReachedEvent(
        UUID userId,
        int streakValue,
        int milestone,
        String milestoneType,
        Instant occurredAt
) implements DomainEvent {
    public StreakMilestoneReachedEvent(UUID userId, int streakValue, int milestone, String milestoneType) {
        this(userId, streakValue, milestone, milestoneType, Instant.now());
    }
}
