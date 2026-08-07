package com.thesystem.modules.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        UUID userId,
        String username,
        String displayName,
        String bio,
        String avatarUrl,
        String timezone,
        String locale,
        String country,
        String accountStatus,
        Instant lastActiveAt,
        Instant createdAt,
        Instant updatedAt
) {
}
