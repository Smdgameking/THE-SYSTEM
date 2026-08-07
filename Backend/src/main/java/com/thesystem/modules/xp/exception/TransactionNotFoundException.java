package com.thesystem.modules.xp.exception;

public class TransactionNotFoundException extends XpException {
    public TransactionNotFoundException(String message) {
        super(message, "TRANSACTION_NOT_FOUND", 404);
    }
}
