package com.thesystem.modules.notification.service.impl;

import com.thesystem.common.exception.BusinessException;
import com.thesystem.modules.notification.dto.NotificationResponse;
import com.thesystem.modules.notification.entity.Notification;
import com.thesystem.modules.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationServiceImpl notificationService;
    private UUID userId;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
        notificationService = new NotificationServiceImpl(notificationRepository);
    }

    @Test
    void shouldCreateNotification() {
        Notification saved = new Notification();
        saved.setId(notificationId);
        saved.setUserId(userId);
        saved.setType("TASK_COMPLETED");
        saved.setTitle("Task completed");
        saved.setMessage("Test task completed");
        saved.setRelatedEntityType("TASK");
        saved.setRelatedEntityId(UUID.randomUUID());
        saved.setRead(false);
        saved.setCreatedAt(Instant.now());
        saved.setUpdatedAt(Instant.now());

        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        NotificationResponse response = notificationService.createNotification(
                userId, "TASK_COMPLETED", "Task completed", "Test task completed", "TASK", UUID.randomUUID(), null
        );

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(notificationId);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.type()).isEqualTo("TASK_COMPLETED");
        assertThat(response.read()).isFalse();

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getType()).isEqualTo("TASK_COMPLETED");
    }

    @Test
    void shouldGetNotificationsForUser() {
        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setUserId(userId);
        notification.setType("TASK_COMPLETED");
        notification.setTitle("Task completed");
        notification.setMessage("Test task completed");
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notification.setUpdatedAt(Instant.now());

        when(notificationRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(notification));

        List<NotificationResponse> result = notificationService.getNotifications(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(notificationId);
        verify(notificationRepository).findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
    }

    @Test
    void shouldGetUnreadNotificationsForUser() {
        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setUserId(userId);
        notification.setType("LEVEL_UP");
        notification.setTitle("Level up!");
        notification.setMessage("You reached level 2");
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notification.setUpdatedAt(Instant.now());

        when(notificationRepository.findByUserIdAndReadAndDeletedAtIsNullOrderByCreatedAtDesc(userId, false))
                .thenReturn(List.of(notification));

        List<NotificationResponse> result = notificationService.getUnreadNotifications(userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo("LEVEL_UP");
    }

    @Test
    void shouldGetNotificationById() {
        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setUserId(userId);
        notification.setType("GOAL_COMPLETED");
        notification.setTitle("Goal completed");
        notification.setMessage("Goal completed");
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notification.setUpdatedAt(Instant.now());

        when(notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notificationId, userId))
                .thenReturn(Optional.of(notification));

        NotificationResponse result = notificationService.getNotification(userId, notificationId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(notificationId);
    }

    @Test
    void shouldThrowWhenNotificationNotFound() {
        when(notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notificationId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.getNotification(userId, notificationId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Notification not found");
    }

    @Test
    void shouldMarkNotificationAsRead() {
        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setUserId(userId);
        notification.setType("ACHIEVEMENT_UNLOCKED");
        notification.setTitle("Achievement unlocked");
        notification.setMessage("First steps unlocked");
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notification.setUpdatedAt(Instant.now());

        when(notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notificationId, userId))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse result = notificationService.markAsRead(userId, notificationId);

        assertThat(result.read()).isTrue();
        assertThat(result.readAt()).isNotNull();
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void shouldNotDuplicateReadTimestamp() {
        Instant existingReadAt = Instant.now().minusSeconds(3600);
        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setUserId(userId);
        notification.setType("ACHIEVEMENT_UNLOCKED");
        notification.setTitle("Achievement unlocked");
        notification.setMessage("First steps unlocked");
        notification.setRead(true);
        notification.setReadAt(existingReadAt);
        notification.setCreatedAt(Instant.now());
        notification.setUpdatedAt(Instant.now());

        when(notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notificationId, userId))
                .thenReturn(Optional.of(notification));

        NotificationResponse result = notificationService.markAsRead(userId, notificationId);

        assertThat(result.read()).isTrue();
        assertThat(result.readAt()).isEqualTo(existingReadAt);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void shouldMarkAllAsRead() {
        Notification notification1 = new Notification();
        notification1.setId(notificationId);
        notification1.setUserId(userId);
        notification1.setType("STREAK_MILESTONE");
        notification1.setTitle("Streak milestone!");
        notification1.setMessage("7-day streak reached");
        notification1.setRead(false);
        notification1.setCreatedAt(Instant.now());
        notification1.setUpdatedAt(Instant.now());

        Notification notification2 = new Notification();
        notification2.setId(UUID.randomUUID());
        notification2.setUserId(userId);
        notification2.setType("TASK_COMPLETED");
        notification2.setTitle("Task completed");
        notification2.setMessage("Task completed");
        notification2.setRead(false);
        notification2.setCreatedAt(Instant.now());
        notification2.setUpdatedAt(Instant.now());

        when(notificationRepository.findByUserIdAndReadAndDeletedAtIsNullOrderByCreatedAtDesc(userId, false))
                .thenReturn(List.of(notification1, notification2));

        notificationService.markAllAsRead(userId);

        assertThat(notification1.isRead()).isTrue();
        assertThat(notification2.isRead()).isTrue();
        assertThat(notification1.getReadAt()).isNotNull();
        assertThat(notification2.getReadAt()).isNotNull();
        verify(notificationRepository).saveAll(List.of(notification1, notification2));
    }

    @Test
    void shouldDeleteNotification() {
        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setUserId(userId);
        notification.setType("TASK_COMPLETED");
        notification.setTitle("Task completed");
        notification.setMessage("Task completed");
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notification.setUpdatedAt(Instant.now());

        when(notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notificationId, userId))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        notificationService.deleteNotification(userId, notificationId);

        assertThat(notification.getDeletedAt()).isNotNull();
        verify(notificationRepository).save(notification);
    }

    @Test
    void shouldCountUnreadNotifications() {
        when(notificationRepository.countByUserIdAndReadAndDeletedAtIsNull(userId, false)).thenReturn(3L);

        long count = notificationService.countUnread(userId);

        assertThat(count).isEqualTo(3L);
        verify(notificationRepository).countByUserIdAndReadAndDeletedAtIsNull(userId, false);
    }

    @Test
    void shouldNotAccessOtherUsersNotifications() {
        UUID otherUserId = UUID.randomUUID();
        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setUserId(otherUserId);
        notification.setType("TASK_COMPLETED");
        notification.setTitle("Task completed");
        notification.setMessage("Task completed");
        notification.setRead(false);
        notification.setCreatedAt(Instant.now());
        notification.setUpdatedAt(Instant.now());

        when(notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(notificationId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.getNotification(userId, notificationId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Notification not found");

        verify(notificationRepository, never()).save(any());
    }
}
