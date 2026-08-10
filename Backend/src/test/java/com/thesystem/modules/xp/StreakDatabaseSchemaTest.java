package com.thesystem.modules.xp;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class StreakDatabaseSchemaTest {

    @Test
    void shouldContainCreateTableStatements() throws Exception {
        ClassPathResource resource = new ClassPathResource("db/migration/V17__create_streak_tables.sql");
        String sql = new BufferedReader(new InputStreamReader(resource.getInputStream()))
                .lines()
                .collect(Collectors.joining("\n"));

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS user_streaks");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS user_streak_history");
        assertThat(sql).contains("user_id UUID NOT NULL");
        assertThat(sql).contains("current_streak INTEGER NOT NULL DEFAULT 0");
        assertThat(sql).contains("longest_streak INTEGER NOT NULL DEFAULT 0");
        assertThat(sql).contains("current_streak_start_date DATE");
        assertThat(sql).contains("last_activity_date DATE");
        assertThat(sql).contains("activity_date DATE NOT NULL");
        assertThat(sql).contains("occurred_at TIMESTAMP NOT NULL");
        assertThat(sql).contains("source_engine VARCHAR(50) NOT NULL");
        assertThat(sql).contains("source_type VARCHAR(50) NOT NULL");
        assertThat(sql).contains("source_id UUID NOT NULL");
        assertThat(sql).contains("deleted_at TIMESTAMP NULL");
        assertThat(sql).contains("CONSTRAINT fk_user_streaks_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE");
        assertThat(sql).contains("CONSTRAINT fk_user_streak_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE");
    }

    @Test
    void shouldContainCheckConstraints() throws Exception {
        ClassPathResource resource = new ClassPathResource("db/migration/V17__create_streak_tables.sql");
        String sql = new BufferedReader(new InputStreamReader(resource.getInputStream()))
                .lines()
                .collect(Collectors.joining("\n"));

        assertThat(sql).contains("chk_current_streak_non_negative");
        assertThat(sql).contains("chk_longest_streak_non_negative");
        assertThat(sql).contains("chk_longest_streak_gte_current");
    }

    @Test
    void shouldContainPartialUniqueIndexes() throws Exception {
        ClassPathResource resource = new ClassPathResource("db/migration/V17__create_streak_tables.sql");
        String sql = new BufferedReader(new InputStreamReader(resource.getInputStream()))
                .lines()
                .collect(Collectors.joining("\n"));

        assertThat(sql).contains("CREATE UNIQUE INDEX IF NOT EXISTS uq_user_streaks_user_id_active");
        assertThat(sql).contains("ON user_streaks(user_id)");
        assertThat(sql).contains("WHERE deleted_at IS NULL");
        assertThat(sql).contains("CREATE UNIQUE INDEX IF NOT EXISTS uq_user_streak_history_source");
        assertThat(sql).contains("ON user_streak_history(source_engine, source_id, source_type)");
    }

    @Test
    void shouldContainRegularIndexes() throws Exception {
        ClassPathResource resource = new ClassPathResource("db/migration/V17__create_streak_tables.sql");
        String sql = new BufferedReader(new InputStreamReader(resource.getInputStream()))
                .lines()
                .collect(Collectors.joining("\n"));

        assertThat(sql).contains("CREATE INDEX IF NOT EXISTS idx_user_streaks_user_id");
        assertThat(sql).contains("CREATE INDEX IF NOT EXISTS idx_user_streaks_last_activity_date");
        assertThat(sql).contains("CREATE INDEX IF NOT EXISTS idx_user_streak_history_user_id");
        assertThat(sql).contains("CREATE INDEX IF NOT EXISTS idx_user_streak_history_user_activity_date");
        assertThat(sql).contains("CREATE INDEX IF NOT EXISTS idx_user_streak_history_activity_date");
        assertThat(sql).contains("CREATE INDEX IF NOT EXISTS idx_user_streak_history_source");
    }

    @Test
    void shouldNotContainUserIdActivityDateUniqueConstraint() throws Exception {
        ClassPathResource resource = new ClassPathResource("db/migration/V17__create_streak_tables.sql");
        String sql = new BufferedReader(new InputStreamReader(resource.getInputStream()))
                .lines()
                .collect(Collectors.joining("\n"));

        assertThat(sql).doesNotContain("UNIQUE (user_id, activity_date)");
    }
}
