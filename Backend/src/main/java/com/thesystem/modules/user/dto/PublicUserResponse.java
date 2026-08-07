package com.thesystem.modules.user.dto;

import java.time.Instant;
import java.util.UUID;

public record PublicUserResponse(
        UUID id,
        String username,
        String displayName,
        String bio,
        String avatarUrl,
        String timezone,
        String locale,
        String country
) {
}
