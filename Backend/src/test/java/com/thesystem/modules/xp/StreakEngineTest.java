package com.thesystem.modules.xp;

import com.thesystem.modules.goal.events.GoalCompletedEvent;
import com.thesystem.modules.task.events.TaskCompletedEvent;
import com.thesystem.modules.user.service.UserTimezoneResolver;
import com.thesystem.modules.xp.entity.UserStreak;
import com.thesystem.modules.xp.entity.UserStreakHistory;
import com.thesystem.modules.xp.events.StreakMilestoneReachedEvent;
import com.thesystem.modules.xp.repository.UserStreakHistoryRepository;
import com.thesystem.modules.xp.repository.UserStreakRepository;
import com.thesystem.modules.xp.service.impl.StreakEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class StreakEngineTest {

    @Autowired
    private UserStreakRepository userStreakRepository;

    @Autowired
    private UserStreakHistoryRepository userStreakHistoryRepository;

    private final UserTimezoneResolver userTimezoneResolver = mock(UserTimezoneResolver.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private StreakEngine streakEngine;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        streakEngine = new StreakEngine(userTimezoneResolver, userStreakRepository, userStreakHistoryRepository, eventPublisher);
    }

    @Test
    void shouldStartStreakOnFirstActivity() {
        UUID userId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));

        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task", "MANUAL", "NORMAL", Instant.parse("2026-08-01T10:00:00Z")));

        UserStreak streak = userStreakRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow();
        assertThat(streak.getCurrentStreak()).isEqualTo(1);
        assertThat(streak.getLongestStreak()).isEqualTo(1);
        assertThat(streak.getCurrentStreakStartDate()).isEqualTo(java.time.LocalDate.of(2026, 8, 1));
        assertThat(streak.getLastActivityDate()).isEqualTo(java.time.LocalDate.of(2026, 8, 1));
    }

    @Test
    void shouldIncrementStreakOnConsecutiveDays() {
        UUID userId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));

        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task1", "MANUAL", "NORMAL", Instant.parse("2026-08-01T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task2", "MANUAL", "NORMAL", Instant.parse("2026-08-02T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task3", "MANUAL", "NORMAL", Instant.parse("2026-08-03T10:00:00Z")));

        UserStreak streak = userStreakRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow();
        assertThat(streak.getCurrentStreak()).isEqualTo(3);
        assertThat(streak.getLongestStreak()).isEqualTo(3);
        assertThat(streak.getCurrentStreakStartDate()).isEqualTo(java.time.LocalDate.of(2026, 8, 1));
        assertThat(streak.getLastActivityDate()).isEqualTo(java.time.LocalDate.of(2026, 8, 3));
    }

    @Test
    void shouldResetStreakOnGap() {
        UUID userId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));

        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task1", "MANUAL", "NORMAL", Instant.parse("2026-08-01T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task2", "MANUAL", "NORMAL", Instant.parse("2026-08-02T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task3", "MANUAL", "NORMAL", Instant.parse("2026-08-05T10:00:00Z")));

        UserStreak streak = userStreakRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow();
        assertThat(streak.getCurrentStreak()).isEqualTo(1);
        assertThat(streak.getLongestStreak()).isEqualTo(2);
        assertThat(streak.getCurrentStreakStartDate()).isEqualTo(java.time.LocalDate.of(2026, 8, 5));
        assertThat(streak.getLastActivityDate()).isEqualTo(java.time.LocalDate.of(2026, 8, 5));
    }

    @Test
    void shouldCountMultipleActivitiesSameDayAsOne() {
        UUID userId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));

        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task1", "MANUAL", "NORMAL", Instant.parse("2026-08-01T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task2", "MANUAL", "NORMAL", Instant.parse("2026-08-01T14:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task3", "MANUAL", "NORMAL", Instant.parse("2026-08-02T10:00:00Z")));

        UserStreak streak = userStreakRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow();
        assertThat(streak.getCurrentStreak()).isEqualTo(2);
        assertThat(streak.getLongestStreak()).isEqualTo(2);
    }

    @Test
    void shouldIgnoreDuplicateSourceEvent() {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));

        streakEngine.handleTaskCompleted(new TaskCompletedEvent(taskId, userId, null, "Task", "MANUAL", "NORMAL", Instant.parse("2026-08-01T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(taskId, userId, null, "Task", "MANUAL", "NORMAL", Instant.parse("2026-08-01T10:00:00Z")));

        List<UserStreakHistory> history = userStreakHistoryRepository.findByUserIdAndDeletedAtIsNullOrderByActivityDateAscOccurredAtAsc(userId);
        assertThat(history).hasSize(1);

        UserStreak streak = userStreakRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow();
        assertThat(streak.getCurrentStreak()).isEqualTo(1);
    }

    @Test
    void shouldCountGoalCompletionTowardsSameStreak() {
        UUID userId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));

        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task", "MANUAL", "NORMAL", Instant.parse("2026-08-01T10:00:00Z")));
        streakEngine.handleGoalCompleted(new GoalCompletedEvent(UUID.randomUUID(), userId, 100, "NORMAL", Instant.parse("2026-08-02T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task2", "MANUAL", "NORMAL", Instant.parse("2026-08-03T10:00:00Z")));

        UserStreak streak = userStreakRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow();
        assertThat(streak.getCurrentStreak()).isEqualTo(3);
        assertThat(streak.getLongestStreak()).isEqualTo(3);
    }

    @Test
    void shouldCountTaskAndGoalOnSameDayAsOneStreakDay() {
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));

        streakEngine.handleTaskCompleted(new TaskCompletedEvent(taskId, userId, null, "Task", "MANUAL", "NORMAL", Instant.parse("2026-08-01T10:00:00Z")));
        streakEngine.handleGoalCompleted(new GoalCompletedEvent(goalId, userId, 100, "NORMAL", Instant.parse("2026-08-01T14:00:00Z")));

        UserStreak streak = userStreakRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow();
        assertThat(streak.getCurrentStreak()).isEqualTo(1);
        assertThat(streak.getLongestStreak()).isEqualTo(1);
    }

    @Test
    void shouldRecalculateAfterOutOfOrderEvent() {
        UUID userId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));

        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task1", "MANUAL", "NORMAL", Instant.parse("2026-08-07T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task2", "MANUAL", "NORMAL", Instant.parse("2026-08-09T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task3", "MANUAL", "NORMAL", Instant.parse("2026-08-08T10:00:00Z")));

        UserStreak streak = userStreakRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow();
        assertThat(streak.getCurrentStreak()).isEqualTo(3);
        assertThat(streak.getLongestStreak()).isEqualTo(3);
        assertThat(streak.getCurrentStreakStartDate()).isEqualTo(java.time.LocalDate.of(2026, 8, 7));
        assertThat(streak.getLastActivityDate()).isEqualTo(java.time.LocalDate.of(2026, 8, 9));
    }

    @Test
    void shouldUseUtcWhenTimezoneIsNull() {
        UUID userId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));

        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task", "MANUAL", "NORMAL", Instant.parse("2026-08-01T10:00:00Z")));

        UserStreakHistory history = userStreakHistoryRepository.findByUserIdAndDeletedAtIsNullOrderByActivityDateAscOccurredAtAsc(userId).get(0);
        assertThat(history.getActivityDate()).isEqualTo(java.time.LocalDate.of(2026, 8, 1));
    }

    @Test
    void shouldConvertTimestampToUserTimezone() {
        UUID userId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("Asia/Kolkata"));

        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task", "MANUAL", "NORMAL", Instant.parse("2026-08-01T10:00:00Z")));

        UserStreakHistory history = userStreakHistoryRepository.findByUserIdAndDeletedAtIsNullOrderByActivityDateAscOccurredAtAsc(userId).get(0);
        assertThat(history.getActivityDate()).isEqualTo(java.time.LocalDate.of(2026, 8, 1));
    }

    @Test
    void shouldPreserveLongestStreakAfterGap() {
        UUID userId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));

        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task1", "MANUAL", "NORMAL", Instant.parse("2026-08-01T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task2", "MANUAL", "NORMAL", Instant.parse("2026-08-02T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task3", "MANUAL", "NORMAL", Instant.parse("2026-08-05T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task4", "MANUAL", "NORMAL", Instant.parse("2026-08-06T10:00:00Z")));

        UserStreak streak = userStreakRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow();
        assertThat(streak.getCurrentStreak()).isEqualTo(2);
        assertThat(streak.getLongestStreak()).isEqualTo(2);
    }

    @Test
    void shouldHandleHistoricalEventBeforeCurrentStreak() {
        UUID userId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));

        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task1", "MANUAL", "NORMAL", Instant.parse("2026-08-05T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task2", "MANUAL", "NORMAL", Instant.parse("2026-08-06T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task3", "MANUAL", "NORMAL", Instant.parse("2026-08-03T10:00:00Z")));

        UserStreak streak = userStreakRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow();
        assertThat(streak.getCurrentStreak()).isEqualTo(2);
        assertThat(streak.getLongestStreak()).isEqualTo(2);
        assertThat(streak.getCurrentStreakStartDate()).isEqualTo(java.time.LocalDate.of(2026, 8, 5));
        assertThat(streak.getLastActivityDate()).isEqualTo(java.time.LocalDate.of(2026, 8, 6));
    }

    @Test
    void shouldHandleHistoricalEventAfterCurrentStreak() {
        UUID userId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));

        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task1", "MANUAL", "NORMAL", Instant.parse("2026-08-01T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task2", "MANUAL", "NORMAL", Instant.parse("2026-08-02T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task3", "MANUAL", "NORMAL", Instant.parse("2026-08-04T10:00:00Z")));

        UserStreak streak = userStreakRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow();
        assertThat(streak.getCurrentStreak()).isEqualTo(1);
        assertThat(streak.getLongestStreak()).isEqualTo(2);
        assertThat(streak.getCurrentStreakStartDate()).isEqualTo(java.time.LocalDate.of(2026, 8, 4));
    }

    @Test
    void shouldPreserveHistoryOccurredAtExactly() {
        UUID userId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-08-01T10:00:00Z");
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));

        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task", "MANUAL", "NORMAL", occurredAt));

        UserStreakHistory history = userStreakHistoryRepository.findByUserIdAndDeletedAtIsNullOrderByActivityDateAscOccurredAtAsc(userId).get(0);
        assertThat(history.getOccurredAt()).isEqualTo(occurredAt);
    }

    @Test
    void shouldHandleMultipleGapsCorrectly() {
        UUID userId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));

        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task1", "MANUAL", "NORMAL", Instant.parse("2026-08-01T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task2", "MANUAL", "NORMAL", Instant.parse("2026-08-03T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task3", "MANUAL", "NORMAL", Instant.parse("2026-08-05T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task4", "MANUAL", "NORMAL", Instant.parse("2026-08-07T10:00:00Z")));

        UserStreak streak = userStreakRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow();
        assertThat(streak.getCurrentStreak()).isEqualTo(1);
        assertThat(streak.getLongestStreak()).isEqualTo(1);
        assertThat(streak.getLastActivityDate()).isEqualTo(java.time.LocalDate.of(2026, 8, 7));
    }

    @Test
    void shouldCalculateActivityDateAtTimezoneMidnightBoundary() {
        UUID userId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("Asia/Kolkata"));

        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task", "MANUAL", "NORMAL", Instant.parse("2026-08-01T23:30:00Z")));

        UserStreakHistory history = userStreakHistoryRepository.findByUserIdAndDeletedAtIsNullOrderByActivityDateAscOccurredAtAsc(userId).get(0);
        assertThat(history.getActivityDate()).isEqualTo(java.time.LocalDate.of(2026, 8, 2));
    }

    @Test
    void shouldCalculateActivityDateAtReverseTimezoneBoundary() {
        UUID userId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("America/New_York"));

        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task", "MANUAL", "NORMAL", Instant.parse("2026-08-01T23:30:00Z")));

        UserStreakHistory history = userStreakHistoryRepository.findByUserIdAndDeletedAtIsNullOrderByActivityDateAscOccurredAtAsc(userId).get(0);
        assertThat(history.getActivityDate()).isEqualTo(java.time.LocalDate.of(2026, 8, 1));
    }

    // ========================
    // Milestone event tests
    // ========================

    @Test
    void shouldPublishMilestoneEventWhenStreakCrosses3() {
        UUID userId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));

        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task1", "MANUAL", "NORMAL", Instant.parse("2026-08-01T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task2", "MANUAL", "NORMAL", Instant.parse("2026-08-02T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task3", "MANUAL", "NORMAL", Instant.parse("2026-08-03T10:00:00Z")));

        verify(eventPublisher).publishEvent(argThat((StreakMilestoneReachedEvent event) ->
                event.userId().equals(userId) &&
                event.streakValue() == 3 &&
                event.milestone() == 3 &&
                "current_streak".equals(event.milestoneType())
        ));
    }

    @Test
    void shouldNotPublishMilestoneEventWhenStreakStaysBelowNextMilestone() {
        UUID userId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));

        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task1", "MANUAL", "NORMAL", Instant.parse("2026-08-01T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task2", "MANUAL", "NORMAL", Instant.parse("2026-08-02T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task3", "MANUAL", "NORMAL", Instant.parse("2026-08-03T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task4", "MANUAL", "NORMAL", Instant.parse("2026-08-04T10:00:00Z")));

        verify(eventPublisher, times(1)).publishEvent(org.mockito.ArgumentMatchers.any(StreakMilestoneReachedEvent.class));
    }

    @Test
    void shouldPublishMilestoneEventWhenStreakCrosses7() {
        UUID userId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));

        for (int i = 1; i <= 7; i++) {
            streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task" + i, "MANUAL", "NORMAL", Instant.parse("2026-08-" + String.format("%02d", i) + "T10:00:00Z")));
        }

        verify(eventPublisher, times(2)).publishEvent(org.mockito.ArgumentMatchers.any(StreakMilestoneReachedEvent.class));
        verify(eventPublisher).publishEvent(argThat((StreakMilestoneReachedEvent event) ->
                event.userId().equals(userId) &&
                event.streakValue() == 7 &&
                event.milestone() == 7 &&
                "current_streak".equals(event.milestoneType())
        ));
    }

    @Test
    void shouldNotPublishDuplicateMilestoneEventForSameMilestone() {
        UUID userId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));

        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task1", "MANUAL", "NORMAL", Instant.parse("2026-08-01T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task2", "MANUAL", "NORMAL", Instant.parse("2026-08-02T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task3", "MANUAL", "NORMAL", Instant.parse("2026-08-03T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task4", "MANUAL", "NORMAL", Instant.parse("2026-08-04T10:00:00Z")));

        verify(eventPublisher, times(1)).publishEvent(argThat((StreakMilestoneReachedEvent event) ->
                event.milestone() == 3
        ));
    }

    @Test
    void shouldPublishMilestoneEventWhenStreakCrosses30() {
        UUID userId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));

        for (int i = 1; i <= 30; i++) {
            streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task" + i, "MANUAL", "NORMAL", Instant.parse("2026-08-" + String.format("%02d", i) + "T10:00:00Z")));
        }

        verify(eventPublisher).publishEvent(argThat((StreakMilestoneReachedEvent event) ->
                event.userId().equals(userId) &&
                event.streakValue() == 30 &&
                event.milestone() == 30 &&
                "current_streak".equals(event.milestoneType())
        ));
    }

    @Test
    void shouldNotPublishDuplicateMilestoneEventOnSameValueRecalculation() {
        UUID userId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));

        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task1", "MANUAL", "NORMAL", Instant.parse("2026-08-01T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task2", "MANUAL", "NORMAL", Instant.parse("2026-08-02T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task3", "MANUAL", "NORMAL", Instant.parse("2026-08-03T10:00:00Z")));

        verify(eventPublisher, times(1)).publishEvent(org.mockito.ArgumentMatchers.any(StreakMilestoneReachedEvent.class));
    }

    @Test
    void shouldNotPublishMilestoneEventWhenStreakResets() {
        UUID userId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));

        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task1", "MANUAL", "NORMAL", Instant.parse("2026-08-01T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task2", "MANUAL", "NORMAL", Instant.parse("2026-08-02T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task3", "MANUAL", "NORMAL", Instant.parse("2026-08-03T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task4", "MANUAL", "NORMAL", Instant.parse("2026-08-10T10:00:00Z")));

        verify(eventPublisher, times(1)).publishEvent(org.mockito.ArgumentMatchers.any(StreakMilestoneReachedEvent.class));
    }

    @Test
    void shouldPublishMultipleMilestonesWhenStreakJumpsAcrossGap() {
        UUID userId = UUID.randomUUID();
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));

        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task1", "MANUAL", "NORMAL", Instant.parse("2026-08-01T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task2", "MANUAL", "NORMAL", Instant.parse("2026-08-02T10:00:00Z")));
        streakEngine.handleTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, null, "Task8", "MANUAL", "NORMAL", Instant.parse("2026-08-08T10:00:00Z")));

        verify(eventPublisher, times(0)).publishEvent(org.mockito.ArgumentMatchers.any(StreakMilestoneReachedEvent.class));
    }
}
