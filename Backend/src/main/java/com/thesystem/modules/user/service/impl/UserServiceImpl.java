package com.thesystem.modules.user.service.impl;

import com.thesystem.common.exception.BusinessException;
import com.thesystem.common.constants.ErrorCodes;
import com.thesystem.modules.user.dto.PublicUserResponse;
import com.thesystem.modules.user.dto.UpdateProfileRequest;
import com.thesystem.modules.user.dto.UserProfileResponse;
import com.thesystem.modules.user.entity.UserProfile;
import com.thesystem.modules.user.events.UserProfileCreatedEvent;
import com.thesystem.modules.user.events.UserProfileUpdatedEvent;
import com.thesystem.modules.user.mapper.UserProfileMapper;
import com.thesystem.modules.user.repository.UserProfileRepository;
import com.thesystem.modules.user.service.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserProfileRepository userProfileRepository;
    private final UserProfileMapper userProfileMapper;
    private final ApplicationEventPublisher eventPublisher;

    public UserServiceImpl(UserProfileRepository userProfileRepository, UserProfileMapper userProfileMapper, ApplicationEventPublisher eventPublisher) {
        this.userProfileRepository = userProfileRepository;
        this.userProfileMapper = userProfileMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public UserProfileResponse getMyProfile(UUID userId) {
        UserProfile profile = userProfileRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Profile not found"));
        updateLastActive(profile);
        return userProfileMapper.toUserProfileResponse(profile);
    }

    @Override
    @Transactional
    public UserProfileResponse updateMyProfile(UUID userId, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseGet(() -> createDefaultProfile(userId));

        if (request.username() != null && !request.username().isBlank()) {
            if (!request.username().equals(profile.getUsername()) &&
                userProfileRepository.existsByUsernameAndDeletedAtIsNull(request.username())) {
                throw new BusinessException(ErrorCodes.CONFLICT, "Username already exists");
            }
            profile.setUsername(request.username().trim());
        }

        if (request.displayName() != null) {
            profile.setDisplayName(request.displayName().trim());
        }
        if (request.bio() != null) {
            profile.setBio(request.bio().trim());
        }
        if (request.avatarUrl() != null) {
            profile.setAvatarUrl(request.avatarUrl().trim());
        }
        if (request.timezone() != null) {
            profile.setTimezone(request.timezone().trim());
        }
        if (request.locale() != null) {
            profile.setLocale(request.locale().trim());
        }
        if (request.country() != null) {
            profile.setCountry(request.country().trim().toUpperCase());
        }

        UserProfile saved = userProfileRepository.save(profile);
        eventPublisher.publishEvent(new UserProfileUpdatedEvent(saved.getId(), saved.getUserId(), saved.getUsername()));
        return userProfileMapper.toUserProfileResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicUserResponse getPublicProfile(UUID userId) {
        UserProfile profile = userProfileRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Profile not found"));
        return userProfileMapper.toPublicUserResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicUserResponse getPublicProfileByUsername(String username) {
        UserProfile profile = userProfileRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Profile not found"));
        return userProfileMapper.toPublicUserResponse(profile);
    }

    @Override
    @Transactional
    public UserProfileResponse createProfileForNewUser(UUID userId, String username) {
        if (userProfileRepository.existsByUsernameAndDeletedAtIsNull(username)) {
            throw new BusinessException(ErrorCodes.CONFLICT, "Username already exists");
        }

        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setUsername(username);
        profile.setAccountStatus("ACTIVE");
        UserProfile saved = userProfileRepository.save(profile);
        eventPublisher.publishEvent(new UserProfileCreatedEvent(saved.getId(), saved.getUserId(), saved.getUsername()));
        return userProfileMapper.toUserProfileResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse findProfileByUsername(String username) {
        UserProfile profile = userProfileRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Profile not found"));
        return userProfileMapper.toUserProfileResponse(profile);
    }

    private UserProfile createDefaultProfile(UUID userId) {
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setAccountStatus("ACTIVE");
        UserProfile saved = userProfileRepository.save(profile);
        eventPublisher.publishEvent(new UserProfileCreatedEvent(saved.getId(), saved.getUserId(), saved.getUsername()));
        return saved;
    }

    private void updateLastActive(UserProfile profile) {
        profile.setLastActiveAt(Instant.now());
        userProfileRepository.save(profile);
    }
}
