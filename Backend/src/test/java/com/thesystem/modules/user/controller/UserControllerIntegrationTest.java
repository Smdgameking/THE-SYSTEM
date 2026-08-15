package com.thesystem.modules.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesystem.common.constants.ErrorCodes;
import com.thesystem.common.exception.BusinessException;
import com.thesystem.modules.user.dto.PublicUserResponse;
import com.thesystem.modules.user.dto.UpdateProfileRequest;
import com.thesystem.modules.user.dto.UserProfileResponse;
import com.thesystem.modules.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {
        static UserService userService = Mockito.mock(UserService.class);

        @Bean
        UserService userService() {
            return userService;
        }
    }

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, null)
        );
    }

    @Test
    void shouldGetMyProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        UserProfileResponse response = new UserProfileResponse(
                UUID.randomUUID(), userId, "johndoe", "John Doe", null, null, null, null, null, "ACTIVE",
                java.time.Instant.now(), java.time.Instant.now(), java.time.Instant.now()
        );

        Mockito.when(TestConfig.userService.getMyProfile(any(UUID.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("johndoe"))
                .andExpect(jsonPath("$.data.displayName").value("John Doe"));
    }

    @Test
    void shouldUpdateMyProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        UpdateProfileRequest request = new UpdateProfileRequest("johndoe", "John Doe", "My bio", null, "UTC", "en", "US");
        UserProfileResponse response = new UserProfileResponse(
                UUID.randomUUID(), userId, "johndoe", "John Doe", "My bio", null, "UTC", "en", "US", "ACTIVE",
                java.time.Instant.now(), java.time.Instant.now(), java.time.Instant.now()
        );

        Mockito.when(TestConfig.userService.updateMyProfile(any(UUID.class), any(UpdateProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("johndoe"))
                .andExpect(jsonPath("$.data.bio").value("My bio"));
    }

    @Test
    void shouldReturnBadRequestForShortUsername() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("ab", "John Doe", null, null, null, null, null);

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldGetPublicProfileById() throws Exception {
        UUID userId = UUID.randomUUID();
        PublicUserResponse response = new PublicUserResponse(
                UUID.randomUUID(), "johndoe", "John Doe", "My bio", null, "UTC", "en", "US"
        );

        Mockito.when(TestConfig.userService.getPublicProfile(userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("johndoe"))
                .andExpect(jsonPath("$.data.displayName").value("John Doe"));
    }

    @Test
    void shouldGetPublicProfileByUsername() throws Exception {
        PublicUserResponse response = new PublicUserResponse(
                UUID.randomUUID(), "johndoe", "John Doe", "My bio", null, "UTC", "en", "US"
        );

        Mockito.when(TestConfig.userService.getPublicProfileByUsername("johndoe")).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/username/johndoe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("johndoe"));
    }

    @Test
    void shouldReturnNotFoundWhenProfileMissing() throws Exception {
        Mockito.when(TestConfig.userService.getMyProfile(any(UUID.class)))
                .thenThrow(new BusinessException(ErrorCodes.NOT_FOUND, "Profile not found"));

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void shouldReturnConflictWhenUsernameTaken() throws Exception {
        Mockito.when(TestConfig.userService.updateMyProfile(any(UUID.class), any(UpdateProfileRequest.class)))
                .thenThrow(new BusinessException(ErrorCodes.CONFLICT, "Username already exists"));

        UpdateProfileRequest request = new UpdateProfileRequest("taken", null, null, null, null, null, null);

        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }
}
