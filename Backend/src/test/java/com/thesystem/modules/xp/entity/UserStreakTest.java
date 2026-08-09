package com.thesystem.modules.xp.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserStreakTest {

    @Test
    void shouldCreateUserStreakWithDefaults() {
        UserStreak streak = new UserStreak();

        assertThat(streak.getCurrentStreak()).isEqualTo(0);
        assertThat(streak.getLongestStreak()).isEqualTo(0);
        assertThat(streak.getCurrentStreakStartDate()).isNull();
        assertThat(streak.getLastActivityDate()).isNull();
    }

    @Test
    void shouldSetAndGetFields() {
        UserStreak streak = new UserStreak();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate lastDate = LocalDate.of(2026, 8, 7);

        streak.setId(id);
        streak.setUserId(userId);
        streak.setCurrentStreak(7);
        streak.setLongestStreak(14);
        streak.setCurrentStreakStartDate(startDate);
        streak.setLastActivityDate(lastDate);

        assertThat(streak.getId()).isEqualTo(id);
        assertThat(streak.getUserId()).isEqualTo(userId);
        assertThat(streak.getCurrentStreak()).isEqualTo(7);
        assertThat(streak.getLongestStreak()).isEqualTo(14);
        assertThat(streak.getCurrentStreakStartDate()).isEqualTo(startDate);
        assertThat(streak.getLastActivityDate()).isEqualTo(lastDate);
    }
}
