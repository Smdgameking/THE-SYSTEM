package com.thesystem.modules.user.service;

import com.thesystem.modules.user.entity.UserProfile;
import com.thesystem.modules.user.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class UserTimezoneResolverTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    private UserTimezoneResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new UserTimezoneResolver(userProfileRepository);
    }

    @Test
    void shouldReturnConfiguredTimezoneWhenValid() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = new UserProfile();
        profile.setTimezone("Asia/Kolkata");

        when(userProfileRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(profile));

        ZoneId zoneId = resolver.resolveUserZoneId(userId);

        assertThat(zoneId).isEqualTo(ZoneId.of("Asia/Kolkata"));
    }

    @Test
    void shouldReturnUtcWhenTimezoneIsNull() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = new UserProfile();
        profile.setTimezone(null);

        when(userProfileRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(profile));

        ZoneId zoneId = resolver.resolveUserZoneId(userId);

        assertThat(zoneId).isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    void shouldReturnUtcWhenTimezoneIsBlank() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = new UserProfile();
        profile.setTimezone("   ");

        when(userProfileRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(profile));

        ZoneId zoneId = resolver.resolveUserZoneId(userId);

        assertThat(zoneId).isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    void shouldReturnUtcWhenTimezoneIsInvalid() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = new UserProfile();
        profile.setTimezone("Invalid/Timezone");

        when(userProfileRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(profile));

        ZoneId zoneId = resolver.resolveUserZoneId(userId);

        assertThat(zoneId).isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    void shouldReturnUtcWhenProfileNotFound() {
        UUID userId = UUID.randomUUID();

        when(userProfileRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        ZoneId zoneId = resolver.resolveUserZoneId(userId);

        assertThat(zoneId).isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    void shouldReturnUtcWhenUserIdIsNull() {
        ZoneId zoneId = resolver.resolveUserZoneId(null);

        assertThat(zoneId).isEqualTo(ZoneId.of("UTC"));
    }

    @Test
    void shouldReturnUtcForDifferentTimezones() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = new UserProfile();
        profile.setTimezone("America/New_York");

        when(userProfileRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(profile));

        ZoneId zoneId = resolver.resolveUserZoneId(userId);

        assertThat(zoneId).isEqualTo(ZoneId.of("America/New_York"));
    }
}
