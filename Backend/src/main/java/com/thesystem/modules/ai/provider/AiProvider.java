package com.thesystem.modules.ai.provider;

import java.util.List;

/**
 * Contract for an AI interaction provider. Concrete providers implement this
 * interface and register as Spring beans; the {@link AiProviderRegistry}
 * collects them by name. v1.0.0 ships with no concrete providers.
 */
public interface AiProvider {

    /**
     * @return stable provider identifier used for configuration
     */
    String name();

    /**
     * @return true when this provider is registered AND able to serve requests
     */
    boolean isConfigured();

    /**
     * Executes an AI interaction. Implementations signal failures by throwing
     * {@link AiProviderException}.
     *
     * @param request the interaction request
     * @return the provider response
     */
    AiProviderResponse generate(AiProviderRequest request);
}
