package com.thesystem.modules.notification.service;

import com.thesystem.modules.notification.dto.NotificationResponse;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    NotificationResponse createNotification(UUID userId, String type, String title, String message, String relatedEntityType, UUID relatedEntityId, String metadata);

    List<NotificationResponse> getNotifications(UUID userId);

    List<NotificationResponse> getUnreadNotifications(UUID userId);

    NotificationResponse getNotification(UUID userId, UUID notificationId);

    NotificationResponse markAsRead(UUID userId, UUID notificationId);

    void markAllAsRead(UUID userId);

    void deleteNotification(UUID userId, UUID notificationId);

    long countUnread(UUID userId);
}
