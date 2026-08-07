package com.thesystem.modules.xp.repository;

import com.thesystem.modules.xp.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, UUID> {

    Optional<UserAchievement> findByUserIdAndAchievementIdAndDeletedAtIsNull(UUID userId, UUID achievementId);

    List<UserAchievement> findByUserIdAndDeletedAtIsNull(UUID userId);

    List<UserAchievement> findByUserIdAndIsUnlockedAndDeletedAtIsNull(UUID userId, Boolean isUnlocked);

    boolean existsByUserIdAndAchievementIdAndDeletedAtIsNull(UUID userId, UUID achievementId);

    long countByUserIdAndIsUnlockedAndDeletedAtIsNull(UUID userId, Boolean isUnlocked);
}
