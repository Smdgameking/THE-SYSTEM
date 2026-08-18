package com.thesystem.modules.goal.service;

import com.thesystem.common.exception.BusinessException;
import com.thesystem.modules.goal.dto.GoalResponse;
import com.thesystem.modules.goal.dto.UpdateGoalRequest;
import com.thesystem.modules.goal.entity.Goal;
import com.thesystem.modules.goal.enums.CompletionStrategy;
import com.thesystem.modules.goal.enums.GoalDifficulty;
import com.thesystem.modules.goal.enums.GoalPriority;
import com.thesystem.modules.goal.enums.GoalStatus;
import com.thesystem.modules.goal.enums.GoalVisibility;
import com.thesystem.modules.goal.events.GoalCompletedEvent;
import com.thesystem.modules.goal.events.GoalProgressUpdatedEvent;
import com.thesystem.modules.goal.mapper.GoalMapper;
import com.thesystem.modules.goal.repository.GoalMilestoneRepository;
import com.thesystem.modules.goal.repository.GoalRepository;
import com.thesystem.modules.goal.service.impl.GoalServiceImpl;
import com.thesystem.modules.task.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoalTaskBasedProgressUnitTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private GoalMilestoneRepository milestoneRepository;

    @Mock
    private GoalMapper goalMapper;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TaskService taskService;

    private GoalServiceImpl goalService;
    private UUID userId;
    private Goal goal;

    @BeforeEach
    void setUp() {
        goalService = new GoalServiceImpl(goalRepository, milestoneRepository, goalMapper, objectMapper, eventPublisher, taskService);
        userId = UUID.randomUUID();
        goal = new Goal();
        goal.setId(UUID.randomUUID());
        goal.setUserId(userId);
        goal.setTitle("Task Goal");
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setPriority(GoalPriority.NORMAL);
        goal.setCompletionStrategy(CompletionStrategy.TASK_BASED);
        goal.setCurrentProgress(0);
        goal.setCompletionPercentage(0.0);
    }

    private void stubMapper() {
        when(goalMapper.toGoalResponse(any(Goal.class))).thenReturn(goalResponse(goal));
    }

    private GoalResponse goalResponse(Goal g) {
        return new GoalResponse(
                g.getId(), userId, g.getTitle(), null, null,
                GoalPriority.NORMAL, GoalDifficulty.NORMAL, g.getStatus(), GoalVisibility.PRIVATE,
                100, g.getCurrentProgress(), g.getCompletionPercentage(), null, g.getCompletedDate(), null,
                g.getCompletionStrategy(), List.of(), Map.of(), Instant.now(), Instant.now()
        );
    }

    @Test
    void shouldRecalculateTaskBasedProgressToPartialValue() {
        stubMapper();
        when(goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goal.getId(), userId)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskService.getGoalTaskProgress(userId, goal.getId())).thenReturn(new TaskService.TaskGoalProgressSnapshot(1, 2));

        goalService.recalculateProgress(userId, goal.getId());

        ArgumentCaptor<Goal> captor = ArgumentCaptor.forClass(Goal.class);
        verify(goalRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentProgress()).isEqualTo(50);
        assertThat(captor.getValue().getCompletionPercentage()).isEqualTo(50.0);
        assertThat(captor.getValue().getStatus()).isEqualTo(GoalStatus.ACTIVE);
    }

    @Test
    void shouldAutoCompleteTaskBasedGoalAtFullTaskCompletion() {
        stubMapper();
        goal.setCurrentProgress(50);
        goal.setCompletionPercentage(50.0);
        when(goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goal.getId(), userId)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskService.getGoalTaskProgress(userId, goal.getId())).thenReturn(new TaskService.TaskGoalProgressSnapshot(2, 2));

        goalService.recalculateProgress(userId, goal.getId());

        ArgumentCaptor<Goal> captor = ArgumentCaptor.forClass(Goal.class);
        verify(goalRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentProgress()).isEqualTo(100);
        assertThat(captor.getValue().getStatus()).isEqualTo(GoalStatus.COMPLETED);
        assertThat(captor.getValue().getCompletedDate()).isNotNull();

        ArgumentCaptor<GoalCompletedEvent> eventCaptor = ArgumentCaptor.forClass(GoalCompletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().goalId()).isEqualTo(goal.getId());
        assertThat(eventCaptor.getValue().userId()).isEqualTo(userId);
    }

    @Test
    void shouldRevertTaskBasedGoalWhenTaskCompletionDropsBelowHundred() {
        stubMapper();
        goal.setStatus(GoalStatus.COMPLETED);
        goal.setCompletedDate(Instant.now());
        goal.setCurrentProgress(100);
        goal.setCompletionPercentage(100.0);
        when(goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goal.getId(), userId)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskService.getGoalTaskProgress(userId, goal.getId())).thenReturn(new TaskService.TaskGoalProgressSnapshot(1, 2));

        goalService.recalculateProgress(userId, goal.getId());

        ArgumentCaptor<Goal> captor = ArgumentCaptor.forClass(Goal.class);
        verify(goalRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentProgress()).isEqualTo(50);
        assertThat(captor.getValue().getStatus()).isEqualTo(GoalStatus.ACTIVE);
        assertThat(captor.getValue().getCompletedDate()).isNull();

        verify(eventPublisher, never()).publishEvent(any(GoalCompletedEvent.class));
        verify(eventPublisher).publishEvent(any(GoalProgressUpdatedEvent.class));
    }

    @Test
    void shouldSetZeroProgressWhenNoLinkedTasks() {
        stubMapper();
        when(goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goal.getId(), userId)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskService.getGoalTaskProgress(userId, goal.getId())).thenReturn(new TaskService.TaskGoalProgressSnapshot(0, 0));

        goalService.recalculateProgress(userId, goal.getId());

        ArgumentCaptor<Goal> captor = ArgumentCaptor.forClass(Goal.class);
        verify(goalRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrentProgress()).isZero();
        assertThat(captor.getValue().getCompletionPercentage()).isZero();
    }

    @Test
    void shouldNotTouchNonTaskBasedGoalsFromTaskEvents() {
        goal.setCompletionStrategy(CompletionStrategy.MANUAL);
        when(goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goal.getId(), userId)).thenReturn(Optional.of(goal));

        goalService.recalculateTaskBasedProgress(userId, goal.getId());

        verify(goalRepository, never()).save(any(Goal.class));
        verify(taskService, never()).getGoalTaskProgress(any(), any());
    }

    @Test
    void shouldNotTouchFailedGoalsFromTaskEvents() {
        goal.setStatus(GoalStatus.FAILED);
        when(goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goal.getId(), userId)).thenReturn(Optional.of(goal));

        goalService.recalculateTaskBasedProgress(userId, goal.getId());

        verify(goalRepository, never()).save(any(Goal.class));
    }

    @Test
    void shouldNotTouchArchivedGoalsFromTaskEvents() {
        goal.setStatus(GoalStatus.ARCHIVED);
        when(goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goal.getId(), userId)).thenReturn(Optional.of(goal));

        goalService.recalculateTaskBasedProgress(userId, goal.getId());

        verify(goalRepository, never()).save(any(Goal.class));
    }

    @Test
    void shouldIgnoreTaskEventsForMissingGoals() {
        when(goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goal.getId(), userId)).thenReturn(Optional.empty());

        goalService.recalculateTaskBasedProgress(userId, goal.getId());

        verify(goalRepository, never()).save(any(Goal.class));
    }

    @Test
    void shouldIgnoreTaskEventsWithNullGoalId() {
        goalService.recalculateTaskBasedProgress(userId, null);

        verify(goalRepository, never()).findByIdAndUserIdAndDeletedAtIsNull(any(), any());
    }

    @Test
    void shouldRejectManualProgressOverrideForTaskBasedGoal() {
        when(goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goal.getId(), userId)).thenReturn(Optional.of(goal));

        assertThatThrownBy(() -> goalService.updateProgress(userId, goal.getId(), 50))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectManualCompletionForTaskBasedGoal() {
        when(goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goal.getId(), userId)).thenReturn(Optional.of(goal));

        assertThatThrownBy(() -> goalService.completeGoal(userId, goal.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRecalculateWhenSwitchingToTaskBasedStrategy() {
        stubMapper();
        goal.setCompletionStrategy(CompletionStrategy.MANUAL);
        when(goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goal.getId(), userId)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskService.getGoalTaskProgress(userId, goal.getId())).thenReturn(new TaskService.TaskGoalProgressSnapshot(1, 2));

        UpdateGoalRequest request = new UpdateGoalRequest(
                null, null, null, null, null, null, null, null,
                CompletionStrategy.TASK_BASED, null, null
        );

        goalService.updateGoal(userId, goal.getId(), request);

        verify(taskService).getGoalTaskProgress(userId, goal.getId());
        ArgumentCaptor<Goal> captor = ArgumentCaptor.forClass(Goal.class);
        verify(goalRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getCompletionStrategy()).isEqualTo(CompletionStrategy.TASK_BASED);
    }
}
