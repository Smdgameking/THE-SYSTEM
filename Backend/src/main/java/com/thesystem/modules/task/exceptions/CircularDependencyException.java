package com.thesystem.modules.task.exceptions;

public class CircularDependencyException extends TaskException {
    public CircularDependencyException(String message) {
        super(message, "CIRCULAR_DEPENDENCY", 400);
    }
}
