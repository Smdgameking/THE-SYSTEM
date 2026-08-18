package com.thesystem.modules.notification.listener;

import com.thesystem.modules.goal.events.GoalCompletedEvent;
import com.thesystem.modules.notification.service.NotificationService;
import com.thesystem.modules.task.events.TaskCompletedEvent;
import com.thesystem.modules.xp.events.AchievementUnlockedEvent;
import com.thesystem.modules.xp.events.LevelUpEvent;
import com.thesystem.modules.xp.events.StreakMilestoneReachedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.Order;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    private NotificationEventListener listener;

    @Test
    void shouldCreateNotificationForTaskCompleted() {
        listener = new NotificationEventListener(notificationService);
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        TaskCompletedEvent event = new TaskCompletedEvent(taskId, userId, null, "Test Task", "MANUAL", "NORMAL", Instant.parse("2026-08-01T10:00:00Z"));

        listener.handleTaskCompleted(event);

        ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(userIdCaptor.capture(), typeCaptor.capture(), any(), any(), any(), any(), any());
        assertThat(userIdCaptor.getValue()).isEqualTo(userId);
        assertThat(typeCaptor.getValue()).isEqualTo("TASK_COMPLETED");
    }

    @Test
    void shouldIgnoreTaskCompletedWithNullUserId() {
        listener = new NotificationEventListener(notificationService);
        TaskCompletedEvent event = new TaskCompletedEvent(UUID.randomUUID(), null, null, "Test Task", "MANUAL", "NORMAL");

        listener.handleTaskCompleted(event);

        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldCreateNotificationForGoalCompleted() {
        listener = new NotificationEventListener(notificationService);
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        GoalCompletedEvent event = new GoalCompletedEvent(goalId, userId, 100, "HARD", Instant.parse("2026-08-01T10:00:00Z"));

        listener.handleGoalCompleted(event);

        ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(userIdCaptor.capture(), typeCaptor.capture(), any(), any(), any(), any(), any());
        assertThat(userIdCaptor.getValue()).isEqualTo(userId);
        assertThat(typeCaptor.getValue()).isEqualTo("GOAL_COMPLETED");
    }

    @Test
    void shouldIgnoreGoalCompletedWithNullUserId() {
        listener = new NotificationEventListener(notificationService);
        GoalCompletedEvent event = new GoalCompletedEvent(UUID.randomUUID(), null, 100, "HARD");

        listener.handleGoalCompleted(event);

        verify(notificationService, never()).createNotification(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldCreateNotificationForAchievementUnlocked() {
        listener = new NotificationEventListener(notificationService);
        UUID userId = UUID.randomUUID();
        UUID achievementId = UUID.randomUUID();
        AchievementUnlockedEvent event = new AchievementUnlockedEvent(userId, achievementId, "first_steps", 50, Instant.parse("2026-08-01T10:00:00Z"));

        listener.handleAchievementUnlocked(event);

        ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(userIdCaptor.capture(), typeCaptor.capture(), any(), any(), any(), any(), any());
        assertThat(userIdCaptor.getValue()).isEqualTo(userId);
        assertThat(typeCaptor.getValue()).isEqualTo("ACHIEVEMENT_UNLOCKED");
    }

    @Test
    void shouldCreateNotificationForLevelUp() {
        listener = new NotificationEventListener(notificationService);
        UUID userId = UUID.randomUUID();
        LevelUpEvent event = new LevelUpEvent(userId, 1, 2, 100, Instant.parse("2026-08-01T10:00:00Z"));

        listener.handleLevelUp(event);

        ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(userIdCaptor.capture(), typeCaptor.capture(), any(), any(), any(), any(), any());
        assertThat(userIdCaptor.getValue()).isEqualTo(userId);
        assertThat(typeCaptor.getValue()).isEqualTo("LEVEL_UP");
    }

    @Test
    void shouldCreateNotificationForStreakMilestone() {
        listener = new NotificationEventListener(notificationService);
        UUID userId = UUID.randomUUID();
        StreakMilestoneReachedEvent event = new StreakMilestoneReachedEvent(userId, 7, 7, "daily", Instant.parse("2026-08-01T10:00:00Z"));

        listener.handleStreakMilestoneReached(event);

        ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(userIdCaptor.capture(), typeCaptor.capture(), any(), any(), any(), any(), any());
        assertThat(userIdCaptor.getValue()).isEqualTo(userId);
        assertThat(typeCaptor.getValue()).isEqualTo("STREAK_MILESTONE");
    }

    @Test
    void shouldHaveHigherOrderThanXpEventListener() {
        Order notificationOrder = NotificationEventListener.class.getAnnotation(Order.class);
        Order xpOrder = com.thesystem.modules.xp.listener.XpEventListener.class.getAnnotation(Order.class);

        assertThat(notificationOrder).isNotNull();
        assertThat(xpOrder).isNotNull();
        assertThat(notificationOrder.value()).isGreaterThan(xpOrder.value());
    }
}
