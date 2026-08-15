package com.thesystem.modules.memory.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesystem.modules.memory.dto.MemoryResponse;
import com.thesystem.modules.memory.entity.Memory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface MemoryMapper {

    @Mapping(target = "tags", source = "tags", qualifiedByName = "stringToList")
    @Mapping(target = "customMetadata", source = "customMetadata", qualifiedByName = "stringToMap")
    MemoryResponse toMemoryResponse(Memory memory);

    @Named("stringToList")
    default List<String> stringToList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return new ObjectMapper().readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Named("stringToMap")
    default Map<String, Object> stringToMap(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return new ObjectMapper().readValue(value, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
