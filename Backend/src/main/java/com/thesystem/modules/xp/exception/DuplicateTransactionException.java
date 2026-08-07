package com.thesystem.modules.xp.exception;

public class DuplicateTransactionException extends XpException {
    public DuplicateTransactionException(String message) {
        super(message, "DUPLICATE_TRANSACTION", 400);
    }
}
