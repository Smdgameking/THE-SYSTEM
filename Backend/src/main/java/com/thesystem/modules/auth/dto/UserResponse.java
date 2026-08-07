package com.thesystem.modules.auth.dto;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        Boolean emailVerified
) {
}
