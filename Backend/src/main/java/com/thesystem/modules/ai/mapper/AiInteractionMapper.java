package com.thesystem.modules.ai.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesystem.modules.ai.dto.AiInteractionResponse;
import com.thesystem.modules.ai.entity.AiInteraction;
import com.thesystem.modules.ai.provider.AiContextItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AiInteractionMapper {

    @Mapping(target = "context", source = "context", qualifiedByName = "stringToContext")
    AiInteractionResponse toResponse(AiInteraction interaction);

    @Named("stringToContext")
    default List<AiContextItem> stringToContext(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return new ObjectMapper().readValue(value, new TypeReference<List<AiContextItem>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }
}
