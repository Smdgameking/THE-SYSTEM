package com.thesystem.modules.memory.dto;

import com.thesystem.modules.memory.enums.MemoryImportance;
import com.thesystem.modules.memory.enums.MemorySource;
import com.thesystem.modules.memory.enums.MemoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CreateMemoryRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @NotBlank(message = "Content is required")
        @Size(max = 10000, message = "Content must not exceed 10000 characters")
        String content,

        MemoryType type,
        MemoryImportance importance,
        MemorySource source,
        UUID sourceId,

        @Size(max = 20, message = "Tags must not exceed 20")
        List<@Size(max = 50, message = "Each tag must not exceed 50 characters") String> tags,

        Map<String, Object> customMetadata
) {
}
