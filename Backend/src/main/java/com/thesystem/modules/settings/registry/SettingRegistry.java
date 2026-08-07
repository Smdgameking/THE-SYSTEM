package com.thesystem.modules.settings.registry;

import com.thesystem.modules.settings.enums.SettingType;
import com.thesystem.modules.settings.enums.Visibility;

import java.util.List;

public interface SettingRegistry {

    void register(SettingDefinition definition);

    void registerAll(List<SettingDefinition> definitions);

    SettingDefinition getDefinition(String namespace, String key);

    List<SettingDefinition> getDefinitionsByNamespace(String namespace);

    List<SettingDefinition> getDefinitionsByOwningEngine(String engine);

    boolean isRegistered(String namespace, String key);

    void validate(String namespace, String key, Object value);

    boolean isLocked();

    void lock();
}
