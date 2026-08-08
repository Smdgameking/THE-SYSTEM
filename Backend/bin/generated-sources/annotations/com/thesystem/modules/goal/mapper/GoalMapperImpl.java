package com.thesystem.modules.goal.mapper;

import com.thesystem.modules.goal.dto.GoalResponse;
import com.thesystem.modules.goal.dto.MilestoneResponse;
import com.thesystem.modules.goal.entity.Goal;
import com.thesystem.modules.goal.entity.GoalMilestone;
import com.thesystem.modules.goal.enums.CompletionStrategy;
import com.thesystem.modules.goal.enums.GoalDifficulty;
import com.thesystem.modules.goal.enums.GoalPriority;
import com.thesystem.modules.goal.enums.GoalStatus;
import com.thesystem.modules.goal.enums.GoalVisibility;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-09T03:54:43+0530",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.100.v20260624-0231, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class GoalMapperImpl implements GoalMapper {

    @Override
    public GoalResponse toGoalResponse(Goal goal) {
        if ( goal == null ) {
            return null;
        }

        List<String> tags = null;
        Map<String, Object> customMetadata = null;
        UUID id = null;
        UUID userId = null;
        String title = null;
        String description = null;
        String category = null;
        GoalPriority priority = null;
        GoalDifficulty difficulty = null;
        GoalStatus status = null;
        GoalVisibility visibility = null;
        Integer estimatedXp = null;
        Integer currentProgress = null;
        Double completionPercentage = null;
        Instant targetDate = null;
        Instant completedDate = null;
        Instant archivedDate = null;
        CompletionStrategy completionStrategy = null;
        Instant createdAt = null;
        Instant updatedAt = null;

        tags = stringToList( goal.getTags() );
        customMetadata = stringToMap( goal.getCustomMetadata() );
        id = goal.getId();
        userId = goal.getUserId();
        title = goal.getTitle();
        description = goal.getDescription();
        category = goal.getCategory();
        priority = goal.getPriority();
        difficulty = goal.getDifficulty();
        status = goal.getStatus();
        visibility = goal.getVisibility();
        estimatedXp = goal.getEstimatedXp();
        currentProgress = goal.getCurrentProgress();
        completionPercentage = goal.getCompletionPercentage();
        targetDate = goal.getTargetDate();
        completedDate = goal.getCompletedDate();
        archivedDate = goal.getArchivedDate();
        completionStrategy = goal.getCompletionStrategy();
        createdAt = goal.getCreatedAt();
        updatedAt = goal.getUpdatedAt();

        GoalResponse goalResponse = new GoalResponse( id, userId, title, description, category, priority, difficulty, status, visibility, estimatedXp, currentProgress, completionPercentage, targetDate, completedDate, archivedDate, completionStrategy, tags, customMetadata, createdAt, updatedAt );

        return goalResponse;
    }

    @Override
    public MilestoneResponse toMilestoneResponse(GoalMilestone milestone) {
        if ( milestone == null ) {
            return null;
        }

        UUID id = null;
        UUID goalId = null;
        String title = null;
        String description = null;
        Integer displayOrder = null;
        Boolean isCompleted = null;
        Instant completedDate = null;
        Instant createdAt = null;
        Instant updatedAt = null;

        id = milestone.getId();
        goalId = milestone.getGoalId();
        title = milestone.getTitle();
        description = milestone.getDescription();
        displayOrder = milestone.getDisplayOrder();
        isCompleted = milestone.getIsCompleted();
        completedDate = milestone.getCompletedDate();
        createdAt = milestone.getCreatedAt();
        updatedAt = milestone.getUpdatedAt();

        MilestoneResponse milestoneResponse = new MilestoneResponse( id, goalId, title, description, displayOrder, isCompleted, completedDate, createdAt, updatedAt );

        return milestoneResponse;
    }
}
