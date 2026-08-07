package com.thesystem.modules.xp.exception;

public class InvalidRewardException extends XpException {
    public InvalidRewardException(String message) {
        super(message, "INVALID_REWARD", 400);
    }
}
