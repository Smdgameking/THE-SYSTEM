package com.thesystem.modules.user.events;

import com.thesystem.shared.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record UserProfileCreatedEvent(
        UUID profileId,
        UUID userId,
        String username,
        Instant occurredAt
) implements DomainEvent {
    public UserProfileCreatedEvent(UUID profileId, UUID userId, String username) {
        this(profileId, userId, username, Instant.now());
    }
}
