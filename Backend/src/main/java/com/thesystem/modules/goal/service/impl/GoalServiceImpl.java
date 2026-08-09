package com.thesystem.modules.goal.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesystem.common.exception.BusinessException;
import com.thesystem.common.constants.ErrorCodes;
import com.thesystem.modules.goal.dto.CreateGoalRequest;
import com.thesystem.modules.goal.dto.CreateMilestoneRequest;
import com.thesystem.modules.goal.dto.GoalDetailResponse;
import com.thesystem.modules.goal.dto.GoalResponse;
import com.thesystem.modules.goal.dto.GoalStatisticsResponse;
import com.thesystem.modules.goal.dto.MilestoneResponse;
import com.thesystem.modules.goal.dto.UpdateGoalRequest;
import com.thesystem.modules.goal.dto.UpdateMilestoneRequest;
import com.thesystem.modules.goal.entity.Goal;
import com.thesystem.modules.goal.entity.GoalMilestone;
import com.thesystem.modules.goal.enums.CompletionStrategy;
import com.thesystem.modules.goal.enums.GoalDifficulty;
import com.thesystem.modules.goal.enums.GoalPriority;
import com.thesystem.modules.goal.enums.GoalStatus;
import com.thesystem.modules.goal.enums.GoalVisibility;
import com.thesystem.modules.goal.events.GoalArchivedEvent;
import com.thesystem.modules.goal.events.GoalCompletedEvent;
import com.thesystem.modules.goal.events.GoalCreatedEvent;
import com.thesystem.modules.goal.events.GoalDeletedEvent;
import com.thesystem.modules.goal.events.GoalPausedEvent;
import com.thesystem.modules.goal.events.GoalProgressUpdatedEvent;
import com.thesystem.modules.goal.events.GoalStartedEvent;
import com.thesystem.modules.goal.events.GoalUpdatedEvent;
import com.thesystem.modules.goal.mapper.GoalMapper;
import com.thesystem.modules.goal.repository.GoalMilestoneRepository;
import com.thesystem.modules.goal.repository.GoalRepository;
import com.thesystem.modules.goal.service.GoalService;
import com.thesystem.security.util.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GoalServiceImpl implements GoalService {

    private final GoalRepository goalRepository;
    private final GoalMilestoneRepository milestoneRepository;
    private final GoalMapper goalMapper;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public GoalServiceImpl(
            GoalRepository goalRepository,
            GoalMilestoneRepository milestoneRepository,
            GoalMapper goalMapper,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher
    ) {
        this.goalRepository = goalRepository;
        this.milestoneRepository = milestoneRepository;
        this.goalMapper = goalMapper;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public GoalResponse createGoal(UUID userId, CreateGoalRequest request) {
        Goal goal = new Goal();
        goal.setUserId(userId);
        goal.setTitle(request.title());
        goal.setDescription(request.description());
        goal.setCategory(request.category());
        goal.setPriority(request.priority() != null ? request.priority() : GoalPriority.NORMAL);
        goal.setDifficulty(request.difficulty());
        goal.setStatus(GoalStatus.DRAFT);
        goal.setVisibility(request.visibility() != null ? request.visibility() : GoalVisibility.PRIVATE);
        goal.setEstimatedXp(request.estimatedXp() != null ? request.estimatedXp() : 0);
        goal.setCurrentProgress(0);
        goal.setCompletionPercentage(0.0);
        goal.setTargetDate(request.targetDate());
        goal.setCompletionStrategy(request.completionStrategy() != null ? request.completionStrategy() : CompletionStrategy.MANUAL);
        goal.setTags(request.tags() != null ? String.join(",", request.tags()) : null);
        goal.setCustomMetadata(toJson(request.customMetadata()));

        Goal saved = goalRepository.save(goal);
        eventPublisher.publishEvent(new GoalCreatedEvent(saved.getId(), userId, saved.getTitle(), saved.getCategory(), saved.getStatus().name()));
        return goalMapper.toGoalResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public GoalDetailResponse getGoal(UUID userId, UUID goalId) {
        Goal goal = goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Goal not found"));
        List<GoalMilestone> milestones = milestoneRepository.findByGoalIdAndDeletedAtIsNullOrderByDisplayOrderAsc(goalId);
        List<MilestoneResponse> milestoneResponses = milestones.stream()
                .map(goalMapper::toMilestoneResponse)
                .toList();
        return goalMapper.toGoalDetailResponse(goal, milestoneResponses);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getGoals(UUID userId, GoalService.GoalFilter filter) {
        List<Goal> goals;
        if (filter.status() != null) {
            goals = goalRepository.findByUserIdAndStatusAndDeletedAtIsNull(userId, filter.status());
        } else {
            goals = goalRepository.findByUserIdAndDeletedAtIsNull(userId);
        }
        return goals.stream()
                .map(goalMapper::toGoalResponse)
                .toList();
    }

    @Override
    @Transactional
    public GoalResponse updateGoal(UUID userId, UUID goalId, UpdateGoalRequest request) {
        Goal goal = goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Goal not found"));

        if (request.title() != null) {
            goal.setTitle(request.title());
        }
        if (request.description() != null) {
            goal.setDescription(request.description());
        }
        if (request.category() != null) {
            goal.setCategory(request.category());
        }
        if (request.priority() != null) {
            goal.setPriority(request.priority());
        }
        if (request.difficulty() != null) {
            goal.setDifficulty(request.difficulty());
        }
        if (request.visibility() != null) {
            goal.setVisibility(request.visibility());
        }
        if (request.estimatedXp() != null) {
            goal.setEstimatedXp(request.estimatedXp());
        }
        if (request.targetDate() != null) {
            goal.setTargetDate(request.targetDate());
        }
        if (request.completionStrategy() != null) {
            goal.setCompletionStrategy(request.completionStrategy());
        }
        if (request.tags() != null) {
            goal.setTags(String.join(",", request.tags()));
        }
        if (request.customMetadata() != null) {
            goal.setCustomMetadata(toJson(request.customMetadata()));
        }

        Goal saved = goalRepository.save(goal);
        eventPublisher.publishEvent(new GoalUpdatedEvent(saved.getId(), userId, saved.getTitle()));
        return goalMapper.toGoalResponse(saved);
    }

    @Override
    @Transactional
    public void deleteGoal(UUID userId, UUID goalId) {
        Goal goal = goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Goal not found"));
        goal.setDeletedAt(Instant.now());
        goalRepository.save(goal);
        eventPublisher.publishEvent(new GoalDeletedEvent(goalId, userId));
    }

    @Override
    @Transactional
    public GoalResponse startGoal(UUID userId, UUID goalId) {
        Goal goal = goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Goal not found"));
        validateTransition(goal.getStatus(), GoalStatus.ACTIVE);
        goal.setStatus(GoalStatus.ACTIVE);
        Goal saved = goalRepository.save(goal);
        eventPublisher.publishEvent(new GoalStartedEvent(saved.getId(), userId));
        return goalMapper.toGoalResponse(saved);
    }

    @Override
    @Transactional
    public GoalResponse pauseGoal(UUID userId, UUID goalId) {
        Goal goal = goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Goal not found"));
        validateTransition(goal.getStatus(), GoalStatus.PAUSED);
        goal.setStatus(GoalStatus.PAUSED);
        Goal saved = goalRepository.save(goal);
        eventPublisher.publishEvent(new GoalPausedEvent(saved.getId(), userId));
        return goalMapper.toGoalResponse(saved);
    }

    @Override
    @Transactional
    public GoalResponse resumeGoal(UUID userId, UUID goalId) {
        Goal goal = goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Goal not found"));
        validateTransition(goal.getStatus(), GoalStatus.ACTIVE);
        goal.setStatus(GoalStatus.ACTIVE);
        Goal saved = goalRepository.save(goal);
        return goalMapper.toGoalResponse(saved);
    }

    @Override
    @Transactional
    public GoalResponse completeGoal(UUID userId, UUID goalId) {
        Goal goal = goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Goal not found"));
        validateTransition(goal.getStatus(), GoalStatus.COMPLETED);
        goal.setStatus(GoalStatus.COMPLETED);
        goal.setCompletedDate(Instant.now());
        goal.setCompletionPercentage(100.0);
        goal.setCurrentProgress(100);
        Goal saved = goalRepository.save(goal);
        eventPublisher.publishEvent(new GoalCompletedEvent(saved.getId(), userId, saved.getEstimatedXp(), saved.getDifficulty() != null ? saved.getDifficulty().name() : null));
        return goalMapper.toGoalResponse(saved);
    }

    @Override
    @Transactional
    public GoalResponse failGoal(UUID userId, UUID goalId, String reason) {
        Goal goal = goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Goal not found"));
        validateTransition(goal.getStatus(), GoalStatus.FAILED);
        goal.setStatus(GoalStatus.FAILED);
        goal.setCustomMetadata(reason != null ? toJson(Map.of("failureReason", reason)) : goal.getCustomMetadata());
        Goal saved = goalRepository.save(goal);
        return goalMapper.toGoalResponse(saved);
    }

    @Override
    @Transactional
    public GoalResponse archiveGoal(UUID userId, UUID goalId) {
        Goal goal = goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Goal not found"));
        validateTransition(goal.getStatus(), GoalStatus.ARCHIVED);
        goal.setStatus(GoalStatus.ARCHIVED);
        goal.setArchivedDate(Instant.now());
        Goal saved = goalRepository.save(goal);
        eventPublisher.publishEvent(new GoalArchivedEvent(saved.getId(), userId));
        return goalMapper.toGoalResponse(saved);
    }

    @Override
    @Transactional
    public GoalResponse updateProgress(UUID userId, UUID goalId, int progress) {
        Goal goal = goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Goal not found"));
        int oldProgress = goal.getCurrentProgress();
        double oldPercentage = goal.getCompletionPercentage();
        goal.setCurrentProgress(Math.max(0, Math.min(100, progress)));
        goal.setCompletionPercentage((double) goal.getCurrentProgress());
        if (goal.getCompletionPercentage() >= 100.0 && goal.getStatus() == GoalStatus.ACTIVE) {
            goal.setStatus(GoalStatus.COMPLETED);
            goal.setCompletedDate(Instant.now());
        }
        Goal saved = goalRepository.save(goal);
        eventPublisher.publishEvent(new GoalProgressUpdatedEvent(
                saved.getId(), userId, oldProgress, saved.getCurrentProgress(),
                oldPercentage, saved.getCompletionPercentage(), "MANUAL"));
        return goalMapper.toGoalResponse(saved);
    }

    @Override
    @Transactional
    public GoalResponse recalculateProgress(UUID userId, UUID goalId) {
        Goal goal = goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Goal not found"));
        int oldProgress = goal.getCurrentProgress();
        double oldPercentage = goal.getCompletionPercentage();

        int newProgress = oldProgress;
        double newPercentage = oldPercentage;

        if (goal.getCompletionStrategy() == CompletionStrategy.MILESTONE_BASED) {
            List<GoalMilestone> milestones = milestoneRepository.findByGoalIdAndDeletedAtIsNullOrderByDisplayOrderAsc(goalId);
            if (!milestones.isEmpty()) {
                long completed = milestones.stream().filter(GoalMilestone::getIsCompleted).count();
                newProgress = (int) Math.round((double) completed / milestones.size() * 100);
                newPercentage = newProgress;
            }
        }

        goal.setCurrentProgress(newProgress);
        goal.setCompletionPercentage(newPercentage);
        Goal saved = goalRepository.save(goal);
        eventPublisher.publishEvent(new GoalProgressUpdatedEvent(
                saved.getId(), userId, oldProgress, saved.getCurrentProgress(),
                oldPercentage, saved.getCompletionPercentage(), "RECALCULATED"));
        return goalMapper.toGoalResponse(saved);
    }

    @Override
    @Transactional
    public MilestoneResponse createMilestone(UUID userId, UUID goalId, CreateMilestoneRequest request) {
        Goal goal = goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Goal not found"));

        Integer maxOrder = milestoneRepository.findByGoalIdAndDeletedAtIsNullOrderByDisplayOrderAsc(goalId)
                .stream()
                .map(GoalMilestone::getDisplayOrder)
                .max(Integer::compareTo)
                .orElse(-1);

        GoalMilestone milestone = new GoalMilestone();
        milestone.setGoalId(goalId);
        milestone.setTitle(request.title());
        milestone.setDescription(request.description());
        milestone.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : maxOrder + 1);
        milestone.setIsCompleted(false);

        GoalMilestone saved = milestoneRepository.save(milestone);
        return goalMapper.toMilestoneResponse(saved);
    }

    @Override
    @Transactional
    public MilestoneResponse updateMilestone(UUID userId, UUID goalId, UUID milestoneId, UpdateMilestoneRequest request) {
        GoalMilestone milestone = milestoneRepository.findByIdAndGoalIdAndDeletedAtIsNull(milestoneId, goalId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Milestone not found"));

        if (request.title() != null) {
            milestone.setTitle(request.title());
        }
        if (request.description() != null) {
            milestone.setDescription(request.description());
        }
        if (request.displayOrder() != null) {
            milestone.setDisplayOrder(request.displayOrder());
        }

        GoalMilestone saved = milestoneRepository.save(milestone);
        return goalMapper.toMilestoneResponse(saved);
    }

    @Override
    @Transactional
    public MilestoneResponse completeMilestone(UUID userId, UUID goalId, UUID milestoneId) {
        GoalMilestone milestone = milestoneRepository.findByIdAndGoalIdAndDeletedAtIsNull(milestoneId, goalId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Milestone not found"));
        milestone.setIsCompleted(true);
        milestone.setCompletedDate(Instant.now());
        GoalMilestone saved = milestoneRepository.save(milestone);
        return goalMapper.toMilestoneResponse(saved);
    }

    @Override
    @Transactional
    public void deleteMilestone(UUID userId, UUID goalId, UUID milestoneId) {
        GoalMilestone milestone = milestoneRepository.findByIdAndGoalIdAndDeletedAtIsNull(milestoneId, goalId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Milestone not found"));
        milestone.setDeletedAt(Instant.now());
        milestoneRepository.save(milestone);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MilestoneResponse> getMilestones(UUID userId, UUID goalId) {
        Goal goal = goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Goal not found"));
        return milestoneRepository.findByGoalIdAndDeletedAtIsNullOrderByDisplayOrderAsc(goalId)
                .stream()
                .map(goalMapper::toMilestoneResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GoalStatisticsResponse getStatistics(UUID userId) {
        List<Goal> allGoals = goalRepository.findByUserIdAndDeletedAtIsNull(userId);
        long total = allGoals.size();
        long active = allGoals.stream().filter(g -> g.getStatus() == GoalStatus.ACTIVE).count();
        long completed = allGoals.stream().filter(g -> g.getStatus() == GoalStatus.COMPLETED).count();
        long failed = allGoals.stream().filter(g -> g.getStatus() == GoalStatus.FAILED).count();
        long archived = allGoals.stream().filter(g -> g.getStatus() == GoalStatus.ARCHIVED).count();
        double avgPercentage = allGoals.stream()
                .mapToDouble(g -> g.getCompletionPercentage())
                .average()
                .orElse(0.0);

        Map<GoalStatus, Long> byStatus = allGoals.stream()
                .collect(java.util.stream.Collectors.groupingBy(Goal::getStatus, java.util.stream.Collectors.counting()));
        Map<GoalPriority, Long> byPriority = allGoals.stream()
                .collect(java.util.stream.Collectors.groupingBy(Goal::getPriority, java.util.stream.Collectors.counting()));

        return new GoalStatisticsResponse(total, active, completed, failed, archived, avgPercentage, byStatus, byPriority);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getGoalsByCategory(UUID userId, String category) {
        return goalRepository.findByUserIdAndCategoryAndDeletedAtIsNull(userId, category)
                .stream()
                .map(goalMapper::toGoalResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getGoalsByStatus(UUID userId, GoalStatus status) {
        return goalRepository.findByUserIdAndStatusAndDeletedAtIsNull(userId, status)
                .stream()
                .map(goalMapper::toGoalResponse)
                .toList();
    }

    private void validateTransition(GoalStatus from, GoalStatus to) {
        boolean allowed = switch (from) {
            case DRAFT -> to == GoalStatus.ACTIVE || to == GoalStatus.ARCHIVED;
            case ACTIVE -> to == GoalStatus.PAUSED || to == GoalStatus.COMPLETED || to == GoalStatus.FAILED || to == GoalStatus.ARCHIVED;
            case PAUSED -> to == GoalStatus.ACTIVE || to == GoalStatus.ARCHIVED;
            case COMPLETED -> to == GoalStatus.ARCHIVED;
            case FAILED -> to == GoalStatus.ACTIVE || to == GoalStatus.ARCHIVED;
            case ARCHIVED -> false;
        };
        if (!allowed) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                    "Invalid state transition from " + from + " to " + to);
        }
    }

    private String toJson(Object obj) {
        try {
            return obj != null ? objectMapper.writeValueAsString(obj) : null;
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Invalid JSON metadata");
        }
    }
}
