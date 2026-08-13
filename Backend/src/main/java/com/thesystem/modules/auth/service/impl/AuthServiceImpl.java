package com.thesystem.modules.auth.service.impl;

import com.thesystem.common.exception.BusinessException;
import com.thesystem.common.constants.ErrorCodes;
import com.thesystem.modules.auth.dto.LoginRequest;
import com.thesystem.modules.auth.dto.RefreshTokenRequest;
import com.thesystem.modules.auth.dto.RegisterRequest;
import com.thesystem.modules.auth.dto.TokenResponse;
import com.thesystem.modules.auth.dto.UserResponse;
import com.thesystem.modules.auth.entity.RefreshToken;
import com.thesystem.modules.auth.entity.Role;
import com.thesystem.modules.auth.entity.User;
import com.thesystem.modules.auth.entity.UserRole;
import com.thesystem.modules.auth.repository.RefreshTokenRepository;
import com.thesystem.modules.auth.repository.RoleRepository;
import com.thesystem.modules.auth.repository.UserRepository;
import com.thesystem.modules.auth.repository.UserRoleRepository;
import com.thesystem.modules.auth.mapper.UserMapper;
import com.thesystem.modules.auth.service.AuthService;
import com.thesystem.modules.user.dto.UserProfileResponse;
import com.thesystem.modules.user.service.UserService;
import com.thesystem.security.service.JwtTokenService;
import com.thesystem.security.service.PasswordEncoderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoderService passwordEncoderService;
    private final JwtTokenService jwtTokenService;
    private final UserMapper userMapper;
    private final UserService userService;

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoderService passwordEncoderService,
            JwtTokenService jwtTokenService,
            UserMapper userMapper,
            UserService userService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoderService = passwordEncoderService;
        this.jwtTokenService = jwtTokenService;
        this.userMapper = userMapper;
        this.userService = userService;
    }

    @Override
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.email())) {
            throw new BusinessException(ErrorCodes.CONFLICT, "Email already exists");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoderService.encode(request.password()));
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

        Role userRole = roleRepository.findByNameAndDeletedAtIsNull("ROLE_USER")
                .orElseGet(() -> {
                    Role newRole = new Role(UUID.randomUUID(), "ROLE_USER", "Default user role");
                    return roleRepository.save(newRole);
                });

        userRoleRepository.save(new UserRole(savedUser.getId(), userRole.getId()));

        userService.createProfileForNewUser(savedUser.getId(), request.username());

        String accessToken = jwtTokenService.generateAccessToken(savedUser.getId(), savedUser.getEmail());
        String refreshToken = jwtTokenService.generateRefreshToken(savedUser.getId());
        saveRefreshToken(savedUser.getId(), refreshToken);

        return new TokenResponse(accessToken, refreshToken, "Bearer", 900L);
    }

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        UserProfileResponse profile;
        try {
            profile = userService.findProfileByUsername(request.username());
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Invalid username or password");
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(profile.userId())
                .orElseThrow(() -> new BusinessException(ErrorCodes.UNAUTHORIZED, "Invalid username or password"));

        if (!passwordEncoderService.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Invalid username or password");
        }

        String accessToken = jwtTokenService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenService.generateRefreshToken(user.getId());
        saveRefreshToken(user.getId(), refreshToken);

        return new TokenResponse(accessToken, refreshToken, "Bearer", 900L);
    }

    @Override
    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        String tokenHash = hashToken(request.refreshToken());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHashAndRevokedFalseAndExpiresAtAfter(tokenHash, Instant.now())
                .orElseThrow(() -> new BusinessException(ErrorCodes.UNAUTHORIZED, "Invalid refresh token"));

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        UUID userId = storedToken.getUserId();
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.UNAUTHORIZED, "User not found"));

        String accessToken = jwtTokenService.generateAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtTokenService.generateRefreshToken(user.getId());
        saveRefreshToken(user.getId(), newRefreshToken);

        return new TokenResponse(accessToken, newRefreshToken, "Bearer", 900L);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        UUID userId;
        try {
            userId = jwtTokenService.getUserIdFromToken(refreshToken);
        } catch (BusinessException e) {
            return;
        }

        List<RefreshToken> activeTokens = refreshTokenRepository.findByUserIdAndRevokedFalse(userId);
        for (RefreshToken token : activeTokens) {
            token.setRevoked(true);
        }
        refreshTokenRepository.saveAll(activeTokens);
    }

    private void saveRefreshToken(UUID userId, String token) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setTokenHash(hashToken(token));
        refreshToken.setExpiresAt(Instant.now().plusMillis(604800000L));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Failed to hash refresh token");
        }
    }
}
