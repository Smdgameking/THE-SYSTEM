package com.thesystem.modules.xp.repository;

import com.thesystem.modules.xp.entity.UserStreakHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserStreakHistoryRepositoryTest {

    @Autowired
    private UserStreakHistoryRepository userStreakHistoryRepository;

    @Test
    void shouldSaveAndFindBySource() {
        UUID sourceId = UUID.randomUUID();
        UserStreakHistory history = new UserStreakHistory();
        history.setUserId(UUID.randomUUID());
        history.setActivityDate(LocalDate.of(2026, 8, 7));
        history.setOccurredAt(Instant.parse("2026-08-07T10:00:00Z"));
        history.setSourceEngine("task-engine");
        history.setSourceType("TASK");
        history.setSourceId(sourceId);

        userStreakHistoryRepository.save(history);

        var found = userStreakHistoryRepository.findBySourceEngineAndSourceIdAndSourceTypeAndDeletedAtIsNull(
                "task-engine", sourceId, "TASK");
        assertThat(found).isPresent();
        assertThat(found.get().getActivityDate()).isEqualTo(LocalDate.of(2026, 8, 7));
    }

    @Test
    void shouldExcludeSoftDeletedHistoryFromSourceLookup() {
        UUID sourceId = UUID.randomUUID();
        UserStreakHistory history = new UserStreakHistory();
        history.setUserId(UUID.randomUUID());
        history.setActivityDate(LocalDate.of(2026, 8, 7));
        history.setOccurredAt(Instant.parse("2026-08-07T10:00:00Z"));
        history.setSourceEngine("task-engine");
        history.setSourceType("TASK");
        history.setSourceId(sourceId);
        history.setDeletedAt(Instant.now());

        userStreakHistoryRepository.save(history);

        var found = userStreakHistoryRepository.findBySourceEngineAndSourceIdAndSourceTypeAndDeletedAtIsNull(
                "task-engine", sourceId, "TASK");
        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindChronologicalHistoryForUser() {
        UUID userId = UUID.randomUUID();
        UserStreakHistory earlier = new UserStreakHistory();
        earlier.setUserId(userId);
        earlier.setActivityDate(LocalDate.of(2026, 8, 5));
        earlier.setOccurredAt(Instant.parse("2026-08-05T10:00:00Z"));
        earlier.setSourceEngine("task-engine");
        earlier.setSourceType("TASK");
        earlier.setSourceId(UUID.randomUUID());

        UserStreakHistory later = new UserStreakHistory();
        later.setUserId(userId);
        later.setActivityDate(LocalDate.of(2026, 8, 7));
        later.setOccurredAt(Instant.parse("2026-08-07T10:00:00Z"));
        later.setSourceEngine("goal-engine");
        later.setSourceType("GOAL");
        later.setSourceId(UUID.randomUUID());

        userStreakHistoryRepository.save(earlier);
        userStreakHistoryRepository.save(later);

        List<UserStreakHistory> history = userStreakHistoryRepository.findByUserIdAndDeletedAtIsNullOrderByOccurredAtAsc(userId);
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getActivityDate()).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(history.get(1).getActivityDate()).isEqualTo(LocalDate.of(2026, 8, 7));
    }

    @Test
    void shouldAllowMultipleActivitiesOnSameDay() {
        UUID userId = UUID.randomUUID();
        UUID taskSourceId = UUID.randomUUID();
        UUID goalSourceId = UUID.randomUUID();

        UserStreakHistory taskHistory = new UserStreakHistory();
        taskHistory.setUserId(userId);
        taskHistory.setActivityDate(LocalDate.of(2026, 8, 7));
        taskHistory.setOccurredAt(Instant.parse("2026-08-07T10:00:00Z"));
        taskHistory.setSourceEngine("task-engine");
        taskHistory.setSourceType("TASK");
        taskHistory.setSourceId(taskSourceId);

        UserStreakHistory goalHistory = new UserStreakHistory();
        goalHistory.setUserId(userId);
        goalHistory.setActivityDate(LocalDate.of(2026, 8, 7));
        goalHistory.setOccurredAt(Instant.parse("2026-08-07T11:00:00Z"));
        goalHistory.setSourceEngine("goal-engine");
        goalHistory.setSourceType("GOAL");
        goalHistory.setSourceId(goalSourceId);

        userStreakHistoryRepository.save(taskHistory);
        userStreakHistoryRepository.save(goalHistory);

        List<UserStreakHistory> dayHistory = userStreakHistoryRepository.findByUserIdAndActivityDateAndDeletedAtIsNull(
                userId, LocalDate.of(2026, 8, 7));
        assertThat(dayHistory).hasSize(2);
    }

    @Test
    void shouldFindHistoryFromDateOnward() {
        UUID userId = UUID.randomUUID();
        UserStreakHistory oldHistory = new UserStreakHistory();
        oldHistory.setUserId(userId);
        oldHistory.setActivityDate(LocalDate.of(2026, 8, 1));
        oldHistory.setOccurredAt(Instant.parse("2026-08-01T10:00:00Z"));
        oldHistory.setSourceEngine("task-engine");
        oldHistory.setSourceType("TASK");
        oldHistory.setSourceId(UUID.randomUUID());

        UserStreakHistory newHistory = new UserStreakHistory();
        newHistory.setUserId(userId);
        newHistory.setActivityDate(LocalDate.of(2026, 8, 7));
        newHistory.setOccurredAt(Instant.parse("2026-08-07T10:00:00Z"));
        newHistory.setSourceEngine("task-engine");
        newHistory.setSourceType("TASK");
        newHistory.setSourceId(UUID.randomUUID());

        userStreakHistoryRepository.save(oldHistory);
        userStreakHistoryRepository.save(newHistory);

        List<UserStreakHistory> fromDate = userStreakHistoryRepository
                .findByUserIdAndActivityDateGreaterThanEqualAndDeletedAtIsNullOrderByActivityDateAsc(
                        userId, LocalDate.of(2026, 8, 5));
        assertThat(fromDate).hasSize(1);
        assertThat(fromDate.get(0).getActivityDate()).isEqualTo(LocalDate.of(2026, 8, 7));
    }
}
