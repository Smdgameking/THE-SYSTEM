package com.thesystem.modules.auth.service.impl;

import com.thesystem.common.exception.BusinessException;
import com.thesystem.common.constants.ErrorCodes;
import com.thesystem.modules.auth.dto.LoginRequest;
import com.thesystem.modules.auth.dto.RefreshTokenRequest;
import com.thesystem.modules.auth.dto.RegisterRequest;
import com.thesystem.modules.auth.dto.TokenResponse;
import com.thesystem.modules.auth.dto.UserResponse;
import com.thesystem.modules.auth.entity.Role;
import com.thesystem.modules.auth.entity.User;
import com.thesystem.modules.auth.entity.UserRole;
import com.thesystem.modules.auth.repository.RoleRepository;
import com.thesystem.modules.auth.repository.UserRepository;
import com.thesystem.modules.auth.repository.UserRoleRepository;
import com.thesystem.modules.auth.mapper.UserMapper;
import com.thesystem.modules.auth.service.AuthService;
import com.thesystem.security.service.JwtTokenService;
import com.thesystem.security.service.PasswordEncoderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoderService passwordEncoderService;
    private final JwtTokenService jwtTokenService;
    private final UserMapper userMapper;

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoderService passwordEncoderService,
            JwtTokenService jwtTokenService,
            UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoderService = passwordEncoderService;
        this.jwtTokenService = jwtTokenService;
        this.userMapper = userMapper;
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

        String accessToken = jwtTokenService.generateAccessToken(savedUser.getId(), savedUser.getEmail());
        String refreshToken = jwtTokenService.generateRefreshToken(savedUser.getId());

        return new TokenResponse(accessToken, refreshToken, "Bearer", 900L);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCodes.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoderService.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Invalid email or password");
        }

        String accessToken = jwtTokenService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenService.generateRefreshToken(user.getId());

        return new TokenResponse(accessToken, refreshToken, "Bearer", 900L);
    }

    @Override
    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        UUID userId;
        try {
            userId = jwtTokenService.getUserIdFromToken(request.refreshToken());
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCodes.UNAUTHORIZED, "Invalid refresh token");
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.UNAUTHORIZED, "User not found"));

        String accessToken = jwtTokenService.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenService.generateRefreshToken(user.getId());

        return new TokenResponse(accessToken, refreshToken, "Bearer", 900L);
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

        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        userRoleRepository.deleteAll(userRoles);
    }
}
