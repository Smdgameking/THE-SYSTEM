package com.thesystem.modules.xp.exception;

public class XpException extends RuntimeException {
    private final String errorCode;
    private final int status;

    public XpException(String message, String errorCode, int status) {
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
