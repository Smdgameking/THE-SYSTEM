package com.thesystem.modules.xp.repository;

import com.thesystem.modules.xp.entity.UserStreakHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class UserStreakHistoryRepositoryTest {

    @Autowired
    private UserStreakHistoryRepository userStreakHistoryRepository;

    @Test
    void shouldSaveAndFindBySource() {
        UUID sourceId = UUID.randomUUID();
        UserStreakHistory history = new UserStreakHistory();
        history.setId(UUID.randomUUID());
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
        history.setId(UUID.randomUUID());
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
        earlier.setId(UUID.randomUUID());
        earlier.setUserId(userId);
        earlier.setActivityDate(LocalDate.of(2026, 8, 5));
        earlier.setOccurredAt(Instant.parse("2026-08-05T10:00:00Z"));
        earlier.setSourceEngine("task-engine");
        earlier.setSourceType("TASK");
        earlier.setSourceId(UUID.randomUUID());

        UserStreakHistory later = new UserStreakHistory();
        later.setId(UUID.randomUUID());
        later.setUserId(userId);
        later.setActivityDate(LocalDate.of(2026, 8, 7));
        later.setOccurredAt(Instant.parse("2026-08-07T10:00:00Z"));
        later.setSourceEngine("goal-engine");
        later.setSourceType("GOAL");
        later.setSourceId(UUID.randomUUID());

        userStreakHistoryRepository.save(earlier);
        userStreakHistoryRepository.save(later);

        List<UserStreakHistory> history = userStreakHistoryRepository.findByUserIdAndDeletedAtIsNullOrderByActivityDateAscOccurredAtAsc(userId);
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getActivityDate()).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(history.get(1).getActivityDate()).isEqualTo(LocalDate.of(2026, 8, 7));
    }

    @Test
    void shouldOrderSameDayActivitiesByOccurredAt() {
        UUID userId = UUID.randomUUID();
        UserStreakHistory morning = new UserStreakHistory();
        morning.setId(UUID.randomUUID());
        morning.setUserId(userId);
        morning.setActivityDate(LocalDate.of(2026, 8, 7));
        morning.setOccurredAt(Instant.parse("2026-08-07T08:00:00Z"));
        morning.setSourceEngine("task-engine");
        morning.setSourceType("TASK");
        morning.setSourceId(UUID.randomUUID());

        UserStreakHistory evening = new UserStreakHistory();
        evening.setId(UUID.randomUUID());
        evening.setUserId(userId);
        evening.setActivityDate(LocalDate.of(2026, 8, 7));
        evening.setOccurredAt(Instant.parse("2026-08-07T18:00:00Z"));
        evening.setSourceEngine("goal-engine");
        evening.setSourceType("GOAL");
        evening.setSourceId(UUID.randomUUID());

        userStreakHistoryRepository.save(morning);
        userStreakHistoryRepository.save(evening);

        List<UserStreakHistory> history = userStreakHistoryRepository.findByUserIdAndDeletedAtIsNullOrderByActivityDateAscOccurredAtAsc(userId);
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getActivityDate()).isEqualTo(LocalDate.of(2026, 8, 7));
        assertThat(history.get(0).getOccurredAt()).isEqualTo(Instant.parse("2026-08-07T08:00:00Z"));
        assertThat(history.get(1).getActivityDate()).isEqualTo(LocalDate.of(2026, 8, 7));
        assertThat(history.get(1).getOccurredAt()).isEqualTo(Instant.parse("2026-08-07T18:00:00Z"));
    }

    @Test
    void shouldAllowMultipleActivitiesOnSameDay() {
        UUID userId = UUID.randomUUID();
        UUID taskSourceId = UUID.randomUUID();
        UUID goalSourceId = UUID.randomUUID();

        UserStreakHistory taskHistory = new UserStreakHistory();
        taskHistory.setId(UUID.randomUUID());
        taskHistory.setUserId(userId);
        taskHistory.setActivityDate(LocalDate.of(2026, 8, 7));
        taskHistory.setOccurredAt(Instant.parse("2026-08-07T10:00:00Z"));
        taskHistory.setSourceEngine("task-engine");
        taskHistory.setSourceType("TASK");
        taskHistory.setSourceId(taskSourceId);

        UserStreakHistory goalHistory = new UserStreakHistory();
        goalHistory.setId(UUID.randomUUID());
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
        oldHistory.setId(UUID.randomUUID());
        oldHistory.setUserId(userId);
        oldHistory.setActivityDate(LocalDate.of(2026, 8, 1));
        oldHistory.setOccurredAt(Instant.parse("2026-08-01T10:00:00Z"));
        oldHistory.setSourceEngine("task-engine");
        oldHistory.setSourceType("TASK");
        oldHistory.setSourceId(UUID.randomUUID());

        UserStreakHistory newHistory = new UserStreakHistory();
        newHistory.setId(UUID.randomUUID());
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
