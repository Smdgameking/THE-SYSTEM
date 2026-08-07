package com.thesystem.modules.settings.mapper;

import com.thesystem.modules.settings.dto.SettingResponse;
import com.thesystem.modules.settings.entity.Setting;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SettingMapper {

    @Mapping(target = "namespace", source = "namespace")
    @Mapping(target = "key", source = "key")
    @Mapping(target = "value", source = "value")
    @Mapping(target = "type", source = "valueType")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "isSystem", source = "isSystem")
    @Mapping(target = "updatedAt", source = "updatedAt")
    SettingResponse toSettingResponse(Setting setting);
}
