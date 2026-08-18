package com.thesystem.modules.ai.provider;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AiProviderRegistryTest {

    private static AiProvider provider(String name) {
        return new AiProvider() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public boolean isConfigured() {
                return true;
            }

            @Override
            public AiProviderResponse generate(AiProviderRequest request) {
                return new AiProviderResponse("ok", null, null, null, null, null);
            }
        };
    }

    @Test
    void shouldFindProviderByName() {
        AiProviderRegistry registry = new AiProviderRegistry(List.of(provider("alpha"), provider("beta")));

        Optional<AiProvider> alpha = registry.find("alpha");
        Optional<AiProvider> beta = registry.find("beta");

        assertThat(alpha).isPresent();
        assertThat(alpha.get().name()).isEqualTo("alpha");
        assertThat(beta).isPresent();
        assertThat(beta.get().name()).isEqualTo("beta");
    }

    @Test
    void shouldReturnEmptyWhenProviderNotFound() {
        AiProviderRegistry registry = new AiProviderRegistry(List.of(provider("alpha")));

        assertThat(registry.find("gamma")).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenNameIsNull() {
        AiProviderRegistry registry = new AiProviderRegistry(List.of(provider("alpha")));

        assertThat(registry.find(null)).isEmpty();
    }

    @Test
    void shouldSupportEmptyProviderSet() {
        AiProviderRegistry registry = new AiProviderRegistry(List.of());

        assertThat(registry.find("anything")).isEmpty();
    }
}
