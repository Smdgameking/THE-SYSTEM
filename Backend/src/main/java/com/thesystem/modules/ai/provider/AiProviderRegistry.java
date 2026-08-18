package com.thesystem.modules.ai.provider;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves configured {@link AiProvider} beans by name. All providers are
 * collected from the Spring context at construction time.
 */
@Component
public class AiProviderRegistry {

    private final Map<String, AiProvider> providers;

    public AiProviderRegistry(List<AiProvider> providers) {
        this.providers = providers.stream()
                .collect(Collectors.toUnmodifiableMap(AiProvider::name, Function.identity()));
    }

    /**
     * @param name the configured provider name
     * @return the registered provider, if present
     */
    public Optional<AiProvider> find(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(providers.get(name));
    }
}
