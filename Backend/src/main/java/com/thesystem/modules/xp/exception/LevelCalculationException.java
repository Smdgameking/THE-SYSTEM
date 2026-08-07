package com.thesystem.modules.xp.exception;

public class LevelCalculationException extends XpException {
    public LevelCalculationException(String message) {
        super(message, "LEVEL_CALCULATION_ERROR", 500);
    }
}
