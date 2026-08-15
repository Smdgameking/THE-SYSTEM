package com.thesystem.modules.memory.dto;

import com.thesystem.modules.memory.enums.MemoryImportance;
import com.thesystem.modules.memory.enums.MemorySource;
import com.thesystem.modules.memory.enums.MemoryType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record MemoryResponse(
        UUID id,
        UUID userId,
        String title,
        String content,
        MemoryType type,
        MemoryImportance importance,
        MemorySource source,
        UUID sourceId,
        List<String> tags,
        Map<String, Object> customMetadata,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy,
        UUID updatedBy,
        Instant deletedAt
) {
}
