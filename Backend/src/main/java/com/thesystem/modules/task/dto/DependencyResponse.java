package com.thesystem.modules.task.dto;

import com.thesystem.modules.task.enums.DependencyStatus;
import com.thesystem.modules.task.enums.DependencyType;

import java.time.Instant;
import java.util.UUID;

public record DependencyResponse(
        UUID id,
        UUID taskId,
        UUID dependsOnTaskId,
        DependencyType dependencyType,
        DependencyStatus status,
        Instant resolvedDate,
        Instant createdAt,
        UUID createdBy,
        Instant deletedAt
) {
}
