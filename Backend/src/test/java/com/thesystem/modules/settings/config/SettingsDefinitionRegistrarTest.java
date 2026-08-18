package com.thesystem.modules.settings.config;

import com.thesystem.modules.settings.enums.SettingType;
import com.thesystem.modules.settings.enums.Visibility;
import com.thesystem.modules.settings.registry.InMemorySettingRegistry;
import com.thesystem.modules.settings.registry.SettingDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.thesystem.common.exception.BusinessException;

class SettingsDefinitionRegistrarTest {

    @Test
    void shouldRegisterDefinitionsAndLockRegistry() {
        InMemorySettingRegistry registry = new InMemorySettingRegistry();
        SettingsDefinitionRegistrar registrar = new SettingsDefinitionRegistrar(registry);

        registrar.register();

        assertThat(registry.isLocked()).isTrue();
        List<SettingDefinition> definitions = registry.getDefinitionsByOwningEngine("settings");
        assertThat(definitions).hasSize(5);
        assertThat(definitions).allSatisfy(definition ->
                assertThat(definition.visibility()).isEqualTo(Visibility.PUBLIC));
        assertThat(registry.getDefinition("appearance", "theme").defaultValue()).isEqualTo("light");
        assertThat(registry.getDefinition("xp", "levelUpNotifications").type()).isEqualTo(SettingType.BOOLEAN);
    }

    @Test
    void shouldRejectRegistrationAfterLock() {
        InMemorySettingRegistry registry = new InMemorySettingRegistry();
        SettingsDefinitionRegistrar registrar = new SettingsDefinitionRegistrar(registry);

        registrar.register();

        SettingDefinition late = new SettingDefinition(
                "appearance", "extra", SettingType.STRING, "x",
                "Late definition", null, Visibility.PUBLIC, "settings"
        );
        assertThrows(BusinessException.class, () -> registry.register(late));
    }
}
