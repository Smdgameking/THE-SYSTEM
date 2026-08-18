package com.thesystem.modules.settings.config;

import com.thesystem.modules.settings.enums.SettingType;
import com.thesystem.modules.settings.enums.Visibility;
import com.thesystem.modules.settings.registry.SettingDefinition;
import com.thesystem.modules.settings.registry.SettingRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Registers all Settings Engine owned definitions during application startup,
 * then locks the registry (Backend Constitution Rules 38, 39, 40; ADR-0004).
 */
@Component
public class SettingsDefinitionRegistrar {

    private final SettingRegistry settingRegistry;

    public SettingsDefinitionRegistrar(SettingRegistry settingRegistry) {
        this.settingRegistry = settingRegistry;
    }

    @PostConstruct
    void register() {
        settingRegistry.registerAll(definitions());
        settingRegistry.lock();
    }

    private List<SettingDefinition> definitions() {
        return List.of(
                new SettingDefinition(
                        "appearance", "theme", SettingType.STRING, "light",
                        "UI theme preference", null, Visibility.PUBLIC, "settings"),
                new SettingDefinition(
                        "appearance", "timezone", SettingType.STRING, "UTC",
                        "Timezone used for XP and activity dates", null, Visibility.PUBLIC, "settings"),
                new SettingDefinition(
                        "notification", "enabled", SettingType.BOOLEAN, true,
                        "Master toggle for in-app notifications", null, Visibility.PUBLIC, "settings"),
                new SettingDefinition(
                        "xp", "levelUpNotifications", SettingType.BOOLEAN, true,
                        "Notify when you reach a new level", null, Visibility.PUBLIC, "settings"),
                new SettingDefinition(
                        "xp", "goalCompletionNotifications", SettingType.BOOLEAN, true,
                        "Notify when a goal is completed", null, Visibility.PUBLIC, "settings")
        );
    }
}
