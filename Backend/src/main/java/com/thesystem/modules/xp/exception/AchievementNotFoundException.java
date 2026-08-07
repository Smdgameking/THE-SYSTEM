package com.thesystem.modules.xp.exception;

public class AchievementNotFoundException extends XpException {
    public AchievementNotFoundException(String message) {
        super(message, "ACHIEVEMENT_NOT_FOUND", 404);
    }
}
