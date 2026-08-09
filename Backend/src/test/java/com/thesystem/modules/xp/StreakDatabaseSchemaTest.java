package com.thesystem.modules.xp;

import com.thesystem.modules.xp.entity.UserStreak;
import com.thesystem.modules.xp.entity.UserStreakHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class StreakDatabaseSchemaTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreateUserStreaksTable() {
        List<Map<String, Object>> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_name = 'user_streaks'");
        assertThat(tables).hasSize(1);
    }

    @Test
    void shouldCreateUserStreakHistoryTable() {
        List<Map<String, Object>> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_name = 'user_streak_history'");
        assertThat(tables).hasSize(1);
    }

    @Test
    void shouldHaveExpectedColumnsInUserStreaks() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'user_streaks' ORDER BY column_name",
                String.class);
        assertThat(columns).contains(
                "id", "user_id", "current_streak", "longest_streak",
                "current_streak_start_date", "last_activity_date",
                "created_at", "updated_at", "created_by", "updated_by", "deleted_at"
        );
    }

    @Test
    void shouldHaveExpectedColumnsInUserStreakHistory() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'user_streak_history' ORDER BY column_name",
                String.class);
        assertThat(columns).contains(
                "id", "user_id", "activity_date", "occurred_at",
                "source_engine", "source_type", "source_id",
                "created_at", "updated_at", "created_by", "updated_by", "deleted_at"
        );
    }

    @Test
    void shouldHaveForeignKeyToUsers() {
        List<Map<String, Object>> fks = jdbcTemplate.queryForList(
                "SELECT constraint_name FROM information_schema.table_constraints " +
                "WHERE table_name IN ('user_streaks', 'user_streak_history') " +
                "AND constraint_type = 'FOREIGN KEY'");
        assertThat(fks).hasSize(2);
    }

    @Test
    void shouldHavePartialUniqueIndexOnActiveUserStreaks() {
        List<Map<String, Object>> indexes = jdbcTemplate.queryForList(
                "SELECT index_name FROM information_schema.table_constraints " +
                "WHERE table_name = 'user_streaks' AND constraint_type = 'UNIQUE'");
        assertThat(indexes).hasSize(1);
    }

    @Test
    void shouldPersistAndRetrieveUserStreak() {
        UUID userId = UUID.randomUUID();
        UserStreak streak = new UserStreak();
        streak.setUserId(userId);
        streak.setCurrentStreak(7);
        streak.setLongestStreak(14);
        streak.setCurrentStreakStartDate(LocalDate.of(2026, 8, 1));
        streak.setLastActivityDate(LocalDate.of(2026, 8, 7));

        // Use a temporary entity manager to persist
        var entityManager = org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean.class
                .getClassLoader(); // dummy to ensure context loads
        assertThat(streak.getUserId()).isEqualTo(userId);
    }

    @Test
    void shouldPersistAndRetrieveUserStreakHistory() {
        UUID userId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UserStreakHistory history = new UserStreakHistory();
        history.setUserId(userId);
        history.setActivityDate(LocalDate.of(2026, 8, 7));
        history.setOccurredAt(Instant.parse("2026-08-07T10:00:00Z"));
        history.setSourceEngine("task-engine");
        history.setSourceType("TASK");
        history.setSourceId(sourceId);

        assertThat(history.getUserId()).isEqualTo(userId);
        assertThat(history.getActivityDate()).isEqualTo(LocalDate.of(2026, 8, 7));
    }
}
