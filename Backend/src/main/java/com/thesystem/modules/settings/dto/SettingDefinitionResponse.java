package com.thesystem.modules.settings.dto;

import com.thesystem.modules.settings.enums.SettingType;
import com.thesystem.modules.settings.enums.Visibility;

import java.time.Instant;

public record SettingDefinitionResponse(
        String namespace,
        String key,
        SettingType type,
        Object defaultValue,
        String description,
        Visibility visibility,
        String owningEngine
) {
}
