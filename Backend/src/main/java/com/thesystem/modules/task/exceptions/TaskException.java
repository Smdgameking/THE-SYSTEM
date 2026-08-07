package com.thesystem.modules.task.exceptions;

public class TaskException extends RuntimeException {
    private final String errorCode;
    private final int status;

    public TaskException(String message, String errorCode, int status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getStatus() {
        return status;
    }
}
