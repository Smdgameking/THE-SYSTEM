package com.thesystem.modules.xp.repository;

import com.thesystem.modules.xp.entity.UserStreak;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class UserStreakRepositoryTest {

    @Autowired
    private UserStreakRepository userStreakRepository;

    @Test
    void shouldSaveAndFindByUserId() {
        UUID userId = UUID.randomUUID();
        UserStreak streak = new UserStreak();
        streak.setId(UUID.randomUUID());
        streak.setUserId(userId);
        streak.setCurrentStreak(5);
        streak.setLongestStreak(10);

        userStreakRepository.save(streak);

        Optional<UserStreak> found = userStreakRepository.findByUserIdAndDeletedAtIsNull(userId);
        assertThat(found).isPresent();
        assertThat(found.get().getCurrentStreak()).isEqualTo(5);
        assertThat(found.get().getLongestStreak()).isEqualTo(10);
    }

    @Test
    void shouldExcludeSoftDeletedStreakFromActiveLookup() {
        UUID userId = UUID.randomUUID();
        UserStreak streak = new UserStreak();
        streak.setId(UUID.randomUUID());
        streak.setUserId(userId);
        streak.setCurrentStreak(5);
        streak.setDeletedAt(java.time.Instant.now());

        userStreakRepository.save(streak);

        Optional<UserStreak> found = userStreakRepository.findByUserIdAndDeletedAtIsNull(userId);
        assertThat(found).isEmpty();
    }

    @Test
    void shouldCheckExistenceByUserId() {
        UUID userId = UUID.randomUUID();
        UserStreak streak = new UserStreak();
        streak.setId(UUID.randomUUID());
        streak.setUserId(userId);

        userStreakRepository.save(streak);

        assertThat(userStreakRepository.existsByUserIdAndDeletedAtIsNull(userId)).isTrue();
    }
}
