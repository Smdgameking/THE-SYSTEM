package com.thesystem.modules.settings.dto;

import com.thesystem.modules.settings.enums.SettingType;

import java.util.Map;

public record NamespaceSettingsResponse(
        String namespace,
        Map<String, SettingResponse> settings
) {
}
