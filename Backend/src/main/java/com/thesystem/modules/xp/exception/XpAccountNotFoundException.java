package com.thesystem.modules.xp.exception;

public class XpAccountNotFoundException extends XpException {
    public XpAccountNotFoundException(String message) {
        super(message, "XP_ACCOUNT_NOT_FOUND", 404);
    }
}
