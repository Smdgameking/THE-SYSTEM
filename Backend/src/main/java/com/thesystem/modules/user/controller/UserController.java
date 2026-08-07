package com.thesystem.modules.user.controller;

import com.thesystem.common.response.ApiResponse;
import com.thesystem.modules.user.dto.PublicUserResponse;
import com.thesystem.modules.user.dto.UpdateProfileRequest;
import com.thesystem.modules.user.dto.UserProfileResponse;
import com.thesystem.modules.user.service.UserService;
import com.thesystem.security.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile() {
        UUID userId = SecurityUtils.getCurrentUserId();
        UserProfileResponse response = userService.getMyProfile(userId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Profile retrieved successfully", UUID.randomUUID().toString()));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        UserProfileResponse response = userService.updateMyProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Profile updated successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PublicUserResponse>> getPublicProfile(@PathVariable UUID id) {
        PublicUserResponse response = userService.getPublicProfile(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Public profile retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<ApiResponse<PublicUserResponse>> getPublicProfileByUsername(@PathVariable String username) {
        PublicUserResponse response = userService.getPublicProfileByUsername(username);
        return ResponseEntity.ok(ApiResponse.ok(response, "Public profile retrieved successfully", UUID.randomUUID().toString()));
    }
}
