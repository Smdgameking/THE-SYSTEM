package com.thesystem.modules.xp.exception;

public class InvalidTransactionException extends XpException {
    public InvalidTransactionException(String message) {
        super(message, "INVALID_TRANSACTION", 400);
    }
}
