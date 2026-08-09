package com.thesystem.modules.user.service;

import com.thesystem.modules.user.entity.UserProfile;
import com.thesystem.modules.user.repository.UserProfileRepository;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserTimezoneResolver {

    private final UserProfileRepository userProfileRepository;

    public UserTimezoneResolver(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public ZoneId resolveUserZoneId(UUID userId) {
        if (userId == null) {
            return ZoneId.of("UTC");
        }

        Optional<UserProfile> profile = userProfileRepository.findByUserIdAndDeletedAtIsNull(userId);
        if (profile.isEmpty()) {
            return ZoneId.of("UTC");
        }

        String timezone = profile.get().getTimezone();
        if (timezone == null || timezone.isBlank()) {
            return ZoneId.of("UTC");
        }

        try {
            return ZoneId.of(timezone.trim());
        } catch (Exception e) {
            return ZoneId.of("UTC");
        }
    }
}
