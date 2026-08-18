package com.thesystem.modules.notification.service.impl;

import com.thesystem.common.exception.BusinessException;
import com.thesystem.modules.notification.dto.NotificationResponse;
import com.thesystem.modules.notification.entity.Notification;
import com.thesystem.modules.notification.repository.NotificationRepository;
import com.thesystem.modules.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public NotificationResponse createNotification(UUID userId, String type, String title, String message, String relatedEntityType, UUID relatedEntityId, String metadata) {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedEntityType(relatedEntityType);
        notification.setRelatedEntityId(relatedEntityId);
        notification.setMetadata(metadata);
        notification.setRead(false);
        notification.setDeletedAt(null);

        Notification saved = notificationRepository.save(notification);
        return toResponse(saved);
    }

    @Override
    public List<NotificationResponse> getNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<NotificationResponse> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndReadAndDeletedAtIsNullOrderByCreatedAtDesc(userId, false)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public NotificationResponse getNotification(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notificationId, userId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Notification not found"));
        return toResponse(notification);
    }

    @Override
    public NotificationResponse markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notificationId, userId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Notification not found"));
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            Notification saved = notificationRepository.save(notification);
            return toResponse(saved);
        }
        return toResponse(notification);
    }

    @Override
    public void markAllAsRead(UUID userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadAndDeletedAtIsNullOrderByCreatedAtDesc(userId, false);
        Instant now = Instant.now();
        for (Notification notification : unread) {
            notification.setRead(true);
            notification.setReadAt(now);
        }
        notificationRepository.saveAll(unread);
    }

    @Override
    public void deleteNotification(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notificationId, userId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Notification not found"));
        notification.setDeletedAt(Instant.now());
        notificationRepository.save(notification);
    }

    @Override
    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndReadAndDeletedAtIsNull(userId, false);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getRelatedEntityType(),
                notification.getRelatedEntityId(),
                notification.getMetadata(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt(),
                notification.getUpdatedAt(),
                notification.getCreatedBy(),
                notification.getUpdatedBy(),
                notification.getDeletedAt()
        );
    }
}
