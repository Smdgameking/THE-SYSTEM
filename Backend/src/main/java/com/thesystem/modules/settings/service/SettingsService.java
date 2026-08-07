package com.thesystem.modules.settings.service;

import com.thesystem.modules.settings.dto.NamespaceSettingsResponse;
import com.thesystem.modules.settings.dto.SettingDefinitionResponse;
import com.thesystem.modules.settings.dto.SettingResponse;
import com.thesystem.modules.settings.registry.SettingDefinition;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SettingsService {

    SettingResponse getSetting(UUID userId, String namespace, String key);

    SettingResponse setSetting(UUID userId, String namespace, String key, Object value);

    void deleteSetting(UUID userId, String namespace, String key);

    NamespaceSettingsResponse getNamespaceSettings(UUID userId, String namespace);

    Map<String, Map<String, SettingResponse>> getAllUserSettings(UUID userId);

    SettingResponse getSystemSetting(String namespace, String key);

    SettingResponse setSystemSetting(String namespace, String key, Object value);

    void registerDefinition(SettingDefinition definition);

    SettingDefinitionResponse getDefinition(String namespace, String key);

    List<SettingDefinitionResponse> getDefinitionsByNamespace(String namespace);

    List<SettingDefinitionResponse> getDefinitionsByOwningEngine(String engine);
}
