package com.thesystem.modules.task.exceptions;

public class InvalidStatusTransitionException extends TaskException {
    public InvalidStatusTransitionException(String message) {
        super(message, "INVALID_STATUS_TRANSITION", 400);
    }
}
