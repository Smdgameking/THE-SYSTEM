package com.thesystem.security.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityUtilsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnNullWhenNoAuthentication() {
        SecurityContextHolder.clearContext();
        assertThat(SecurityUtils.getCurrentUserId()).isNull();
    }

    @Test
    void shouldReturnNullWhenPrincipalIsNull() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(null, null)
        );
        assertThat(SecurityUtils.getCurrentUserId()).isNull();
    }

    @Test
    void shouldReturnNullWhenPrincipalIsNotValidUUID() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("not-a-uuid", null)
        );
        assertThat(SecurityUtils.getCurrentUserId()).isNull();
    }

    @Test
    void shouldReturnNullWhenPrincipalIsEmptyString() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("", null)
        );
        assertThat(SecurityUtils.getCurrentUserId()).isNull();
    }

    @Test
    void shouldReturnValidUUIDWhenPrincipalIsValidUUIDString() {
        UUID userId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null)
        );
        assertThat(SecurityUtils.getCurrentUserId()).isEqualTo(userId);
    }
}
