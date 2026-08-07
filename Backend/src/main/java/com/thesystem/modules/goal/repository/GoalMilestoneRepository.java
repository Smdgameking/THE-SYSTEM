package com.thesystem.modules.goal.repository;

import com.thesystem.modules.goal.entity.GoalMilestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalMilestoneRepository extends JpaRepository<GoalMilestone, UUID> {

    List<GoalMilestone> findByGoalIdAndDeletedAtIsNullOrderByDisplayOrderAsc(UUID goalId);

    Optional<GoalMilestone> findByIdAndGoalIdAndDeletedAtIsNull(UUID id, UUID goalId);

    boolean existsByIdAndGoalIdAndDeletedAtIsNull(UUID id, UUID goalId);

    List<GoalMilestone> findByGoalIdAndIsCompletedAndDeletedAtIsNull(UUID goalId, Boolean isCompleted);
}
