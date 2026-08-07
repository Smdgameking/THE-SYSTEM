package com.thesystem.modules.goal.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesystem.modules.goal.dto.GoalDetailResponse;
import com.thesystem.modules.goal.dto.GoalResponse;
import com.thesystem.modules.goal.dto.MilestoneResponse;
import com.thesystem.modules.goal.entity.Goal;
import com.thesystem.modules.goal.entity.GoalMilestone;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface GoalMapper {

    @Mapping(target = "tags", source = "tags", qualifiedByName = "stringToList")
    @Mapping(target = "customMetadata", source = "customMetadata", qualifiedByName = "stringToMap")
    GoalResponse toGoalResponse(Goal goal);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "goalId", source = "goalId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "displayOrder", source = "displayOrder")
    @Mapping(target = "isCompleted", source = "isCompleted")
    @Mapping(target = "completedDate", source = "completedDate")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    MilestoneResponse toMilestoneResponse(GoalMilestone milestone);

    default GoalDetailResponse toGoalDetailResponse(Goal goal, List<MilestoneResponse> milestones) {
        return new GoalDetailResponse(
                goal.getId(),
                goal.getUserId(),
                goal.getTitle(),
                goal.getDescription(),
                goal.getCategory(),
                goal.getPriority(),
                goal.getDifficulty(),
                goal.getStatus(),
                goal.getVisibility(),
                goal.getEstimatedXp(),
                goal.getCurrentProgress(),
                goal.getCompletionPercentage(),
                goal.getTargetDate(),
                goal.getCompletedDate(),
                goal.getArchivedDate(),
                goal.getCompletionStrategy(),
                stringToList(goal.getTags()),
                stringToMap(goal.getCustomMetadata()),
                goal.getCreatedAt(),
                goal.getUpdatedAt(),
                milestones
        );
    }

    @org.mapstruct.Named("stringToList")
    default List<String> stringToList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.asList(value.split(","));
    }

    @org.mapstruct.Named("stringToMap")
    default Map<String, Object> stringToMap(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(value, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }
}
