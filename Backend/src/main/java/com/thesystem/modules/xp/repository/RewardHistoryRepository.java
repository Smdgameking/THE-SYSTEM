package com.thesystem.modules.xp.repository;

import com.thesystem.modules.xp.entity.RewardHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RewardHistoryRepository extends JpaRepository<RewardHistory, UUID> {

    List<RewardHistory> findByUserIdAndDeletedAtIsNull(UUID userId);

    List<RewardHistory> findByUserIdAndAwardedAtBetweenAndDeletedAtIsNull(UUID userId, Instant start, Instant end);

    List<RewardHistory> findByUserIdAndSourceTypeAndSourceIdAndDeletedAtIsNull(UUID userId, String sourceType, UUID sourceId);

    List<RewardHistory> findByUserIdAndDeletedAtIsNullOrderByAwardedAtDesc(UUID userId);
}
