package com.thesystem.modules.user.service;

import com.thesystem.modules.user.dto.PublicUserResponse;
import com.thesystem.modules.user.dto.UpdateProfileRequest;
import com.thesystem.modules.user.dto.UserProfileResponse;

import java.util.UUID;

public interface UserService {

    UserProfileResponse getMyProfile(UUID userId);

    UserProfileResponse updateMyProfile(UUID userId, UpdateProfileRequest request);

    PublicUserResponse getPublicProfile(UUID userId);

    PublicUserResponse getPublicProfileByUsername(String username);
}
