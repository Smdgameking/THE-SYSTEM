package com.thesystem.modules.xp.repository;

import com.thesystem.modules.xp.entity.UserStreakHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserStreakHistoryRepository extends JpaRepository<UserStreakHistory, UUID> {

    Optional<UserStreakHistory> findBySourceEngineAndSourceIdAndSourceTypeAndDeletedAtIsNull(
            String sourceEngine, UUID sourceId, String sourceType);

    boolean existsBySourceEngineAndSourceIdAndSourceTypeAndDeletedAtIsNull(
            String sourceEngine, UUID sourceId, String sourceType);

    List<UserStreakHistory> findByUserIdAndDeletedAtIsNullOrderByActivityDateAscOccurredAtAsc(UUID userId);

    List<UserStreakHistory> findByUserIdAndActivityDateAndDeletedAtIsNull(UUID userId, LocalDate activityDate);

    List<UserStreakHistory> findByUserIdAndActivityDateGreaterThanEqualAndDeletedAtIsNullOrderByActivityDateAsc(
            UUID userId, LocalDate activityDate);
}
