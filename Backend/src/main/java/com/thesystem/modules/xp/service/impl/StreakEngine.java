package com.thesystem.modules.xp.service.impl;

import com.thesystem.modules.goal.events.GoalCompletedEvent;
import com.thesystem.modules.task.events.TaskCompletedEvent;
import com.thesystem.modules.user.service.UserTimezoneResolver;
import com.thesystem.modules.xp.entity.UserStreak;
import com.thesystem.modules.xp.entity.UserStreakHistory;
import com.thesystem.modules.xp.events.StreakMilestoneReachedEvent;
import com.thesystem.modules.xp.repository.UserStreakHistoryRepository;
import com.thesystem.modules.xp.repository.UserStreakRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StreakEngine {

    private static final String SOURCE_ENGINE_TASK = "task-engine";
    private static final String SOURCE_TYPE_TASK = "TASK_COMPLETION";
    private static final String SOURCE_ENGINE_GOAL = "goal-engine";
    private static final String SOURCE_TYPE_GOAL = "GOAL_COMPLETION";
    private static final int[] STREAK_MILESTONES = {3, 7, 14, 30, 60, 90};
    private static final String POSTGRES_UNIQUE_VIOLATION = "23505";

    /*
     * Concurrency note: user_streaks has a potential lost-update race under concurrent
     * same-user completions. Two threads can read the same materialized streak state,
     * compute new values, and write back, losing one update. History remains authoritative
     * enough for future recalculation. Consider optimistic locking or row locking before
     * streak values are used for XP multiplier calculation or achievement evaluation.
     */

    private final UserTimezoneResolver userTimezoneResolver;
    private final UserStreakRepository userStreakRepository;
    private final UserStreakHistoryRepository userStreakHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    public StreakEngine(
            UserTimezoneResolver userTimezoneResolver,
            UserStreakRepository userStreakRepository,
            UserStreakHistoryRepository userStreakHistoryRepository,
            ApplicationEventPublisher eventPublisher) {
        this.userTimezoneResolver = userTimezoneResolver;
        this.userStreakRepository = userStreakRepository;
        this.userStreakHistoryRepository = userStreakHistoryRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void handleTaskCompleted(TaskCompletedEvent event) {
        if (event == null || event.taskId() == null || event.userId() == null || event.occurredAt() == null) {
            return;
        }
        processQualifyingEvent(
                event.userId(),
                event.taskId(),
                event.occurredAt(),
                SOURCE_ENGINE_TASK,
                SOURCE_TYPE_TASK
        );
    }

    @Transactional
    public void handleGoalCompleted(GoalCompletedEvent event) {
        if (event == null || event.goalId() == null || event.userId() == null || event.occurredAt() == null) {
            return;
        }
        processQualifyingEvent(
                event.userId(),
                event.goalId(),
                event.occurredAt(),
                SOURCE_ENGINE_GOAL,
                SOURCE_TYPE_GOAL
        );
    }

    private void processQualifyingEvent(UUID userId, UUID sourceId, java.time.Instant occurredAt, String sourceEngine, String sourceType) {
        ZoneId zoneId = userTimezoneResolver.resolveUserZoneId(userId);
        LocalDate activityDate = occurredAt.atZone(zoneId).toLocalDate();

        if (userStreakHistoryRepository.existsBySourceEngineAndSourceIdAndSourceTypeAndDeletedAtIsNull(sourceEngine, sourceId, sourceType)) {
            return;
        }

        UserStreakHistory history = new UserStreakHistory();
        history.setId(UUID.randomUUID());
        history.setUserId(userId);
        history.setActivityDate(activityDate);
        history.setOccurredAt(occurredAt);
        history.setSourceEngine(sourceEngine);
        history.setSourceType(sourceType);
        history.setSourceId(sourceId);

        try {
            userStreakHistoryRepository.save(history);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateKeyViolation(e)) {
                return;
            }
            throw e;
        }

        recalculateStreak(userId);
    }

    private boolean isDuplicateKeyViolation(DataIntegrityViolationException e) {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof SQLException sqlException) {
                String sqlState = sqlException.getSQLState();
                if (POSTGRES_UNIQUE_VIOLATION.equals(sqlState)) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }

    private void recalculateStreak(UUID userId) {
        List<UserStreakHistory> allHistory = userStreakHistoryRepository
                .findByUserIdAndDeletedAtIsNullOrderByActivityDateAscOccurredAtAsc(userId);

        if (allHistory.isEmpty()) {
            return;
        }

        List<LocalDate> uniqueDates = allHistory.stream()
                .map(UserStreakHistory::getActivityDate)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        int currentStreak = 1;
        int longestStreak = 1;
        LocalDate currentStreakStartDate = uniqueDates.get(0);
        LocalDate lastActivityDate = uniqueDates.get(uniqueDates.size() - 1);

        for (int i = 1; i < uniqueDates.size(); i++) {
            LocalDate prev = uniqueDates.get(i - 1);
            LocalDate curr = uniqueDates.get(i);

            if (curr.equals(prev.plusDays(1))) {
                currentStreak++;
            } else {
                currentStreak = 1;
                currentStreakStartDate = curr;
            }

            if (currentStreak > longestStreak) {
                longestStreak = currentStreak;
            }
        }

        UserStreak streak = userStreakRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseGet(() -> {
                    UserStreak newStreak = new UserStreak();
                    newStreak.setId(UUID.randomUUID());
                    newStreak.setUserId(userId);
                    return newStreak;
                });

        int previousStreak = streak.getCurrentStreak() != null ? streak.getCurrentStreak() : 0;

        streak.setCurrentStreak(currentStreak);
        streak.setLongestStreak(longestStreak);
        streak.setCurrentStreakStartDate(currentStreakStartDate);
        streak.setLastActivityDate(lastActivityDate);

        userStreakRepository.save(streak);

        publishMilestoneEvents(userId, previousStreak, currentStreak);
    }

    private void publishMilestoneEvents(UUID userId, int previousStreak, int currentStreak) {
        for (int milestone : STREAK_MILESTONES) {
            if (previousStreak < milestone && currentStreak >= milestone) {
                eventPublisher.publishEvent(
                        new StreakMilestoneReachedEvent(userId, currentStreak, milestone, "current_streak")
                );
            }
        }
    }
}
