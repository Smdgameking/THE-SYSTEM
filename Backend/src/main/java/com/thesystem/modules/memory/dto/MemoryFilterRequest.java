package com.thesystem.modules.memory.dto;

import com.thesystem.modules.memory.enums.MemoryImportance;
import com.thesystem.modules.memory.enums.MemorySource;
import com.thesystem.modules.memory.enums.MemoryType;

public record MemoryFilterRequest(
        MemoryType type,
        MemoryImportance importance,
        MemorySource source,
        String search,
        String sortBy,
        String sortOrder,
        Integer page,
        Integer limit
) {
}
