package com.thesystem.modules.auth.service;

import com.thesystem.common.exception.BusinessException;
import com.thesystem.common.constants.ErrorCodes;
import com.thesystem.modules.auth.dto.LoginRequest;
import com.thesystem.modules.auth.dto.RefreshTokenRequest;
import com.thesystem.modules.auth.dto.RegisterRequest;
import com.thesystem.modules.auth.dto.TokenResponse;
import com.thesystem.modules.auth.entity.Role;
import com.thesystem.modules.auth.entity.User;
import com.thesystem.modules.auth.repository.RoleRepository;
import com.thesystem.modules.auth.repository.UserRepository;
import com.thesystem.modules.auth.repository.UserRoleRepository;
import com.thesystem.modules.auth.service.impl.AuthServiceImpl;
import com.thesystem.security.service.JwtTokenService;
import com.thesystem.security.service.PasswordEncoderService;
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
class AuthServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoderService passwordEncoderService;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private com.thesystem.modules.auth.mapper.UserMapper userMapper;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository, roleRepository, userRoleRepository,
                passwordEncoderService, jwtTokenService, userMapper
        );
    }

    @Test
    void shouldRegisterUserSuccessfully() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123");

        when(userRepository.existsByEmailAndDeletedAtIsNull("test@example.com")).thenReturn(false);
        when(passwordEncoderService.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        Role userRole = new Role(UUID.randomUUID(), "ROLE_USER", "Default user role");
        when(roleRepository.findByNameAndDeletedAtIsNull("ROLE_USER")).thenReturn(Optional.of(userRole));

        when(jwtTokenService.generateAccessToken(any(UUID.class), any(String.class))).thenReturn("accessToken");
        when(jwtTokenService.generateRefreshToken(any(UUID.class))).thenReturn("refreshToken");

        TokenResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("accessToken");
        assertThat(response.refreshToken()).isEqualTo("refreshToken");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);

        verify(userRepository).save(any(User.class));
        verify(userRoleRepository).save(any());
    }

    @Test
    void shouldThrowConflictWhenEmailExists() {
        RegisterRequest request = new RegisterRequest("existing@example.com", "password123");

        when(userRepository.existsByEmailAndDeletedAtIsNull("existing@example.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldLoginSuccessfully() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        User user = new User(UUID.randomUUID(), "test@example.com", "hashedPassword", false);

        when(userRepository.findByEmailAndDeletedAtIsNull("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoderService.matches("password123", "hashedPassword")).thenReturn(true);
        when(jwtTokenService.generateAccessToken(user.getId(), user.getEmail())).thenReturn("accessToken");
        when(jwtTokenService.generateRefreshToken(user.getId())).thenReturn("refreshToken");

        TokenResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("accessToken");
        assertThat(response.refreshToken()).isEqualTo("refreshToken");
    }

    @Test
    void shouldThrowUnauthorizedWhenLoginWithInvalidEmail() {
        LoginRequest request = new LoginRequest("nonexistent@example.com", "password123");

        when(userRepository.findByEmailAndDeletedAtIsNull("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> authService.login(request));
    }

    @Test
    void shouldRefreshTokenSuccessfully() {
        UUID userId = UUID.randomUUID();
        RefreshTokenRequest request = new RefreshTokenRequest("validRefreshToken");
        User user = new User(userId, "test@example.com", "hashedPassword", false);

        when(jwtTokenService.getUserIdFromToken("validRefreshToken")).thenReturn(userId);
        when(userRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(user));
        when(jwtTokenService.generateAccessToken(userId, user.getEmail())).thenReturn("newAccessToken");
        when(jwtTokenService.generateRefreshToken(userId)).thenReturn("newRefreshToken");

        TokenResponse response = authService.refreshToken(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("newAccessToken");
        assertThat(response.refreshToken()).isEqualTo("newRefreshToken");
    }
}
