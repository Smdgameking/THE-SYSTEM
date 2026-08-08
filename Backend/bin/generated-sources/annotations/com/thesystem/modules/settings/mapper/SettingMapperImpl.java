package com.thesystem.modules.settings.mapper;

import com.thesystem.modules.settings.dto.SettingResponse;
import com.thesystem.modules.settings.entity.Setting;
import com.thesystem.modules.settings.enums.SettingType;
import java.time.Instant;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-09T04:49:22+0530",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.100.v20260624-0231, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class SettingMapperImpl implements SettingMapper {

    @Override
    public SettingResponse toSettingResponse(Setting setting) {
        if ( setting == null ) {
            return null;
        }

        String namespace = null;
        String key = null;
        Object value = null;
        SettingType type = null;
        String description = null;
        Boolean isSystem = null;
        Instant updatedAt = null;

        namespace = setting.getNamespace();
        key = setting.getKey();
        value = setting.getValue();
        type = setting.getValueType();
        description = setting.getDescription();
        isSystem = setting.getIsSystem();
        updatedAt = setting.getUpdatedAt();

        SettingResponse settingResponse = new SettingResponse( namespace, key, value, type, description, isSystem, updatedAt );

        return settingResponse;
    }
}
