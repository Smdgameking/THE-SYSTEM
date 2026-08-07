package com.thesystem.modules.xp.repository;

import com.thesystem.modules.xp.entity.AchievementDefinition;
import com.thesystem.modules.xp.enums.AchievementCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AchievementDefinitionRepository extends JpaRepository<AchievementDefinition, UUID> {

    Optional<AchievementDefinition> findByCodeAndDeletedAtIsNull(String code);

    List<AchievementDefinition> findByCategoryAndDeletedAtIsNull(AchievementCategory category);

    List<AchievementDefinition> findByIsHiddenAndDeletedAtIsNull(Boolean isHidden);

    List<AchievementDefinition> findByDeletedAtIsNullOrderBySortOrderAsc();
}
