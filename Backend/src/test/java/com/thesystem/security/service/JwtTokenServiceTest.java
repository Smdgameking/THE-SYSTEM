package com.thesystem.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService();
        ReflectionTestUtils.setField(jwtTokenService, "jwtSecret",
                "test-secret-that-is-at-least-32-characters-long-for-signing");
        ReflectionTestUtils.setField(jwtTokenService, "accessTokenExpirationMs", 900000L);
        ReflectionTestUtils.setField(jwtTokenService, "refreshTokenExpirationMs", 604800000L);
        jwtTokenService.validateSecret();
    }

    @Test
    void refreshTokensForSameUserWithinSameSecondAreUnique() {
        UUID userId = UUID.randomUUID();

        String first = jwtTokenService.generateRefreshToken(userId);
        String second = jwtTokenService.generateRefreshToken(userId);

        assertThat(first).isNotEqualTo(second);
    }
}
