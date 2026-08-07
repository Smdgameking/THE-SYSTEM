package com.thesystem.common.exception;

public class BusinessException extends RuntimeException {
    private final String errorCode;
    private final java.util.Map<String, Object> details;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = java.util.Collections.emptyMap();
    }

    public BusinessException(String errorCode, String message, java.util.Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details != null ? details : java.util.Collections.emptyMap();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public java.util.Map<String, Object> getDetails() {
        return details;
    }
}
