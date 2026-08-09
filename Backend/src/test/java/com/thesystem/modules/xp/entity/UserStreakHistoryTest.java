package com.thesystem.modules.xp.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserStreakHistoryTest {

    @Test
    void shouldCreateUserStreakHistoryWithDefaults() {
        UserStreakHistory history = new UserStreakHistory();

        assertThat(history.getUserId()).isNull();
        assertThat(history.getActivityDate()).isNull();
        assertThat(history.getOccurredAt()).isNull();
        assertThat(history.getSourceEngine()).isNull();
        assertThat(history.getSourceType()).isNull();
        assertThat(history.getSourceId()).isNull();
    }

    @Test
    void shouldSetAndGetFields() {
        UserStreakHistory history = new UserStreakHistory();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        LocalDate activityDate = LocalDate.of(2026, 8, 7);
        Instant occurredAt = Instant.parse("2026-08-07T10:00:00Z");

        history.setId(id);
        history.setUserId(userId);
        history.setActivityDate(activityDate);
        history.setOccurredAt(occurredAt);
        history.setSourceEngine("task-engine");
        history.setSourceType("TASK");
        history.setSourceId(sourceId);

        assertThat(history.getId()).isEqualTo(id);
        assertThat(history.getUserId()).isEqualTo(userId);
        assertThat(history.getActivityDate()).isEqualTo(activityDate);
        assertThat(history.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(history.getSourceEngine()).isEqualTo("task-engine");
        assertThat(history.getSourceType()).isEqualTo("TASK");
        assertThat(history.getSourceId()).isEqualTo(sourceId);
    }
}
