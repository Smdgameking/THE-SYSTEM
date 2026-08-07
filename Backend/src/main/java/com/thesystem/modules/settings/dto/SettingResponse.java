package com.thesystem.modules.settings.dto;

import com.thesystem.modules.settings.enums.SettingType;

import java.time.Instant;

public record SettingResponse(
        String namespace,
        String key,
        Object value,
        SettingType type,
        String description,
        Boolean isSystem,
        Instant updatedAt
) {
}
