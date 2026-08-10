package com.thesystem.modules.xp;

import com.thesystem.modules.goal.events.GoalCompletedEvent;
import com.thesystem.modules.task.events.TaskCompletedEvent;
import com.thesystem.modules.user.service.UserTimezoneResolver;
import com.thesystem.modules.xp.entity.UserStreak;
import com.thesystem.modules.xp.entity.UserStreakHistory;
import com.thesystem.modules.xp.repository.UserStreakHistoryRepository;
import com.thesystem.modules.xp.repository.UserStreakRepository;
import com.thesystem.modules.xp.service.impl.StreakEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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
    private StreakEngine streakEngine;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        streakEngine = new StreakEngine(userTimezoneResolver, userStreakRepository, userStreakHistoryRepository);
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
}
