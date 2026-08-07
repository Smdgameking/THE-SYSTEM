package com.thesystem.modules.task.exceptions;

public class DependencyValidationException extends TaskException {
    public DependencyValidationException(String message) {
        super(message, "INVALID_DEPENDENCY", 400);
    }
}
