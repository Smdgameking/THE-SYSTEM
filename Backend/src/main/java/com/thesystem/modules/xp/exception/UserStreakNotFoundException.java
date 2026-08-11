package com.thesystem.modules.xp.exception;

public class UserStreakNotFoundException extends XpException {
    public UserStreakNotFoundException(String message) {
        super(message, "USER_STREAK_NOT_FOUND", 404);
    }
}
