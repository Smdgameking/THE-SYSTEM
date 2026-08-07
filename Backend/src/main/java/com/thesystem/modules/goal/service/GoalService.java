package com.thesystem.modules.goal.service;

import com.thesystem.modules.goal.dto.CreateGoalRequest;
import com.thesystem.modules.goal.dto.CreateMilestoneRequest;
import com.thesystem.modules.goal.dto.GoalDetailResponse;
import com.thesystem.modules.goal.dto.GoalResponse;
import com.thesystem.modules.goal.dto.GoalStatisticsResponse;
import com.thesystem.modules.goal.dto.MilestoneResponse;
import com.thesystem.modules.goal.dto.UpdateGoalRequest;
import com.thesystem.modules.goal.dto.UpdateMilestoneRequest;

import com.thesystem.modules.goal.enums.CompletionStrategy;
import com.thesystem.modules.goal.enums.GoalDifficulty;
import com.thesystem.modules.goal.enums.GoalPriority;
import com.thesystem.modules.goal.enums.GoalStatus;

import java.util.List;
import java.util.UUID;

public interface GoalService {

    GoalResponse createGoal(UUID userId, CreateGoalRequest request);

    GoalDetailResponse getGoal(UUID userId, UUID goalId);

    List<GoalResponse> getGoals(UUID userId, GoalFilter filter);

    GoalResponse updateGoal(UUID userId, UUID goalId, UpdateGoalRequest request);

    void deleteGoal(UUID userId, UUID goalId);

    GoalResponse startGoal(UUID userId, UUID goalId);

    GoalResponse pauseGoal(UUID userId, UUID goalId);

    GoalResponse resumeGoal(UUID userId, UUID goalId);

    GoalResponse completeGoal(UUID userId, UUID goalId);

    GoalResponse failGoal(UUID userId, UUID goalId, String reason);

    GoalResponse archiveGoal(UUID userId, UUID goalId);

    GoalResponse updateProgress(UUID userId, UUID goalId, int progress);

    GoalResponse recalculateProgress(UUID userId, UUID goalId);

    MilestoneResponse createMilestone(UUID userId, UUID goalId, CreateMilestoneRequest request);

    MilestoneResponse updateMilestone(UUID userId, UUID goalId, UUID milestoneId, UpdateMilestoneRequest request);

    MilestoneResponse completeMilestone(UUID userId, UUID goalId, UUID milestoneId);

    void deleteMilestone(UUID userId, UUID goalId, UUID milestoneId);

    List<MilestoneResponse> getMilestones(UUID userId, UUID goalId);

    GoalStatisticsResponse getStatistics(UUID userId);

    List<GoalResponse> getGoalsByCategory(UUID userId, String category);

    List<GoalResponse> getGoalsByStatus(UUID userId, GoalStatus status);

    record GoalFilter(
            GoalStatus status,
            GoalPriority priority,
            String category,
            GoalDifficulty difficulty,
            int page,
            int size
    ) {
    }
}
