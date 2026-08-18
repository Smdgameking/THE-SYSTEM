package com.thesystem.modules.ai.provider;

/**
 * Signals a failure inside an AI provider. Thrown by {@link AiProvider}
 * implementations; the AI service maps it to SERVICE_UNAVAILABLE.
 */
public class AiProviderException extends RuntimeException {

    public AiProviderException(String message) {
        super(message);
    }

    public AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
