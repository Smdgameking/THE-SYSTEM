package com.thesystem.modules.notification.dto;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID userId,
        String type,
        String title,
        String message,
        String relatedEntityType,
        UUID relatedEntityId,
        String metadata,
        boolean read,
        Instant readAt,
        Instant createdAt,
        Instant updatedAt,
        UUID createdBy,
        UUID updatedBy,
        Instant deletedAt
) {
}
