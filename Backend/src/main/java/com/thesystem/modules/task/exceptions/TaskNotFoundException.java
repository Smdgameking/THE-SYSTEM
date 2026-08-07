package com.thesystem.modules.task.exceptions;

public class TaskNotFoundException extends TaskException {
    public TaskNotFoundException(String message) {
        super(message, "TASK_NOT_FOUND", 404);
    }
}
