package com.thesystem.modules.user.service;

import com.thesystem.common.exception.BusinessException;
import com.thesystem.common.constants.ErrorCodes;
import com.thesystem.modules.user.dto.PublicUserResponse;
import com.thesystem.modules.user.dto.UpdateProfileRequest;
import com.thesystem.modules.user.dto.UserProfileResponse;
import com.thesystem.modules.user.entity.UserProfile;
import com.thesystem.modules.user.mapper.UserProfileMapper;
import com.thesystem.modules.user.repository.UserProfileRepository;
import com.thesystem.modules.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserProfileMapper userProfileMapper;

    private UserServiceImpl userService;

    private UUID userId;
    private UserProfile profile;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userProfileRepository, userProfileMapper);
        userId = UUID.randomUUID();
        profile = new UserProfile(UUID.randomUUID(), userId, "johndoe", "John Doe");
    }

    @Test
    void shouldGetMyProfileWhenExists() {
        when(userProfileRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(profile);
        when(userProfileMapper.toUserProfileResponse(profile)).thenReturn(new UserProfileResponse(
                profile.getId(), userId, "johndoe", "John Doe", null, null, null, null, null, "ACTIVE", Instant.now(), Instant.now(), Instant.now()
        ));

        UserProfileResponse response = userService.getMyProfile(userId);

        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("johndoe");
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    @Test
    void shouldCreateDefaultProfileWhenNotExists() {
        when(userProfileRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(invocation -> {
            UserProfile p = invocation.getArgument(0);
            if (p.getId() == null) {
                p.setId(UUID.randomUUID());
            }
            return p;
        });
        when(userProfileMapper.toUserProfileResponse(any(UserProfile.class))).thenReturn(new UserProfileResponse(
                UUID.randomUUID(), userId, null, null, null, null, null, null, null, "ACTIVE", Instant.now(), Instant.now(), Instant.now()
        ));

        UserProfileResponse response = userService.getMyProfile(userId);

        assertThat(response).isNotNull();
        verify(userProfileRepository, times(2)).save(any(UserProfile.class));
    }

    @Test
    void shouldUpdateProfileSuccessfully() {
        when(userProfileRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(profile);
        when(userProfileMapper.toUserProfileResponse(profile)).thenReturn(new UserProfileResponse(
                profile.getId(), userId, "newusername", "John Doe", null, null, null, null, null, "ACTIVE", Instant.now(), Instant.now(), Instant.now()
        ));

        UpdateProfileRequest request = new UpdateProfileRequest("newusername", "John Doe", null, null, null, null, null);
        UserProfileResponse response = userService.updateMyProfile(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("newusername");
        verify(userProfileRepository).save(profile);
    }

    @Test
    void shouldThrowConflictWhenUsernameExists() {
        when(userProfileRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(profile));
        when(userProfileRepository.existsByUsernameAndDeletedAtIsNull("existinguser")).thenReturn(true);

        UpdateProfileRequest request = new UpdateProfileRequest("existinguser", "John Doe", null, null, null, null, null);

        assertThrows(BusinessException.class, () -> userService.updateMyProfile(userId, request));
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void shouldGetPublicProfile() {
        when(userProfileRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(profile));
        when(userProfileMapper.toPublicUserResponse(profile)).thenReturn(new PublicUserResponse(
                profile.getId(), "johndoe", "John Doe", null, null, null, null, null
        ));

        PublicUserResponse response = userService.getPublicProfile(userId);

        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("johndoe");
    }

    @Test
    void shouldThrowNotFoundWhenPublicProfileDoesNotExist() {
        when(userProfileRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> userService.getPublicProfile(userId));
    }

    @Test
    void shouldGetPublicProfileByUsername() {
        when(userProfileRepository.findByUsernameAndDeletedAtIsNull("johndoe")).thenReturn(Optional.of(profile));
        when(userProfileMapper.toPublicUserResponse(profile)).thenReturn(new PublicUserResponse(
                profile.getId(), "johndoe", "John Doe", null, null, null, null, null
        ));

        PublicUserResponse response = userService.getPublicProfileByUsername("johndoe");

        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("johndoe");
    }
}
