package com.thesystem.modules.xp.exception;

public class PolicyNotFoundException extends XpException {
    public PolicyNotFoundException(String message) {
        super(message, "POLICY_NOT_FOUND", 404);
    }
}
