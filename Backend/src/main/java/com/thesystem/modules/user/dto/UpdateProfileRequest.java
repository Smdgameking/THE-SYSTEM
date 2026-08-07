package com.thesystem.modules.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @Size(max = 100, message = "Display name must not exceed 100 characters")
        String displayName,

        @Size(max = 500, message = "Bio must not exceed 500 characters")
        String bio,

        @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
        String avatarUrl,

        @Size(max = 50, message = "Timezone must not exceed 50 characters")
        String timezone,

        @Size(max = 10, message = "Locale must not exceed 10 characters")
        String locale,

        @Size(max = 2, message = "Country code must not exceed 2 characters")
        String country
) {
}
