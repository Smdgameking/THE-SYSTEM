package com.thesystem.modules.goal.repository;

import com.thesystem.modules.goal.entity.Goal;
import com.thesystem.modules.goal.enums.GoalPriority;
import com.thesystem.modules.goal.enums.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    List<Goal> findByUserIdAndDeletedAtIsNull(UUID userId);

    Optional<Goal> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    boolean existsByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    List<Goal> findByUserIdAndStatusAndDeletedAtIsNull(UUID userId, GoalStatus status);

    List<Goal> findByUserIdAndCategoryAndDeletedAtIsNull(UUID userId, String category);

    List<Goal> findByUserIdAndPriorityAndDeletedAtIsNull(UUID userId, GoalPriority priority);
}
