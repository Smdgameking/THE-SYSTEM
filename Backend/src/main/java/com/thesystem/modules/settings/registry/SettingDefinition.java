package com.thesystem.modules.settings.registry;

import com.thesystem.modules.settings.enums.SettingType;
import com.thesystem.modules.settings.enums.Visibility;

import java.util.function.Predicate;

public record SettingDefinition(
        String namespace,
        String key,
        SettingType type,
        Object defaultValue,
        String description,
        Predicate<Object> validator,
        Visibility visibility,
        String owningEngine
) {
}
