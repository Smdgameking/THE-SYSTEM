package com.thesystem.modules.goal.service;

import com.thesystem.common.exception.BusinessException;
import com.thesystem.common.constants.ErrorCodes;
import com.thesystem.modules.goal.dto.CreateGoalRequest;
import com.thesystem.modules.goal.dto.CreateMilestoneRequest;
import com.thesystem.modules.goal.dto.GoalDetailResponse;
import com.thesystem.modules.goal.dto.GoalResponse;
import com.thesystem.modules.goal.dto.GoalStatisticsResponse;
import com.thesystem.modules.goal.dto.MilestoneResponse;
import com.thesystem.modules.goal.dto.UpdateGoalRequest;
import com.thesystem.modules.goal.entity.Goal;
import com.thesystem.modules.goal.entity.GoalMilestone;
import com.thesystem.modules.goal.enums.CompletionStrategy;
import com.thesystem.modules.goal.enums.GoalDifficulty;
import com.thesystem.modules.goal.enums.GoalPriority;
import com.thesystem.modules.goal.enums.GoalStatus;
import com.thesystem.modules.goal.enums.GoalVisibility;
import com.thesystem.modules.goal.mapper.GoalMapper;
import com.thesystem.modules.goal.repository.GoalMilestoneRepository;
import com.thesystem.modules.goal.repository.GoalRepository;
import com.thesystem.modules.goal.service.impl.GoalServiceImpl;
import com.thesystem.modules.goal.events.GoalCreatedEvent;
import com.thesystem.modules.goal.events.GoalUpdatedEvent;
import com.thesystem.modules.goal.events.GoalStartedEvent;
import com.thesystem.modules.goal.events.GoalPausedEvent;
import com.thesystem.modules.goal.events.GoalCompletedEvent;
import com.thesystem.modules.goal.events.GoalArchivedEvent;
import com.thesystem.modules.goal.events.GoalDeletedEvent;
import com.thesystem.modules.goal.events.GoalProgressUpdatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalServiceUnitTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private GoalMilestoneRepository milestoneRepository;

    @Mock
    private GoalMapper goalMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private GoalServiceImpl goalService;
    private UUID userId;
    private Goal goal;

    @BeforeEach
    void setUp() {
        goalService = new GoalServiceImpl(goalRepository, milestoneRepository, goalMapper, objectMapper, eventPublisher);
        userId = UUID.randomUUID();
        goal = new Goal();
        goal.setId(UUID.randomUUID());
        goal.setUserId(userId);
        goal.setTitle("Test Goal");
        goal.setStatus(GoalStatus.DRAFT);
        goal.setPriority(GoalPriority.NORMAL);
        goal.setCurrentProgress(0);
        goal.setCompletionPercentage(0.0);
    }

    @Test
    void shouldCreateGoalSuccessfully() {
        CreateGoalRequest request = new CreateGoalRequest(
                "Test Goal", "Description", "Category", GoalPriority.NORMAL,
                GoalDifficulty.EASY, com.thesystem.modules.goal.enums.GoalType.LONG_TERM,
                GoalVisibility.PRIVATE, 100, Instant.now(), CompletionStrategy.MANUAL,
                List.of("tag1"), Map.of()
        );

        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> {
            Goal g = invocation.getArgument(0);
            g.setId(UUID.randomUUID());
            return g;
        });
        when(goalMapper.toGoalResponse(any(Goal.class))).thenReturn(new GoalResponse(
                goal.getId(), userId, "Test Goal", "Description", "Category",
                GoalPriority.NORMAL, GoalDifficulty.EASY, GoalStatus.DRAFT, GoalVisibility.PRIVATE,
                100, 0, 0.0, Instant.now(), null, null, CompletionStrategy.MANUAL,
                List.of("tag1"), Map.of(), Instant.now(), Instant.now()
        ));

        GoalResponse response = goalService.createGoal(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Test Goal");
        verify(goalRepository).save(any(Goal.class));
        verify(eventPublisher).publishEvent(any(GoalCreatedEvent.class));
    }

    @Test
    void shouldGetGoalSuccessfully() {
        UUID goalId = UUID.randomUUID();
        goal.setId(goalId);
        when(goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, userId)).thenReturn(Optional.of(goal));
        when(milestoneRepository.findByGoalIdAndDeletedAtIsNullOrderByDisplayOrderAsc(goalId)).thenReturn(List.of());
        when(goalMapper.toGoalDetailResponse(any(Goal.class), any())).thenReturn(new GoalDetailResponse(
                goalId, userId, "Test Goal", "Description", "Category", GoalPriority.NORMAL, GoalDifficulty.EASY,
                GoalStatus.DRAFT, GoalVisibility.PRIVATE, 100, 0, 0.0, Instant.now(), null, null,
                CompletionStrategy.MANUAL, List.of(), Map.of(), Instant.now(), Instant.now(), List.of()
        ));

        GoalDetailResponse response = goalService.getGoal(userId, goalId);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Test Goal");
    }

    @Test
    void shouldThrowNotFoundWhenGoalDoesNotExist() {
        UUID goalId = UUID.randomUUID();
        when(goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, userId)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> goalService.getGoal(userId, goalId));
    }

    @Test
    void shouldStartGoalSuccessfully() {
        UUID goalId = UUID.randomUUID();
        goal.setId(goalId);
        when(goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, userId)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(goalMapper.toGoalResponse(any(Goal.class))).thenReturn(new GoalResponse(
                goalId, userId, "Test Goal", "Description", "Category",
                GoalPriority.NORMAL, GoalDifficulty.EASY, GoalStatus.ACTIVE, GoalVisibility.PRIVATE,
                100, 0, 0.0, Instant.now(), null, null, CompletionStrategy.MANUAL,
                List.of(), Map.of(), Instant.now(), Instant.now()
        ));

        GoalResponse response = goalService.startGoal(userId, goalId);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(GoalStatus.ACTIVE);
        verify(eventPublisher).publishEvent(any(GoalStartedEvent.class));
    }

    @Test
    void shouldCompleteGoalSuccessfully() {
        UUID goalId = UUID.randomUUID();
        goal.setId(goalId);
        goal.setStatus(GoalStatus.ACTIVE);
        when(goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, userId)).thenReturn(Optional.of(goal));
        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(goalMapper.toGoalResponse(any(Goal.class))).thenReturn(new GoalResponse(
                goalId, userId, "Test Goal", "Description", "Category",
                GoalPriority.NORMAL, GoalDifficulty.EASY, GoalStatus.COMPLETED, GoalVisibility.PRIVATE,
                100, 100, 100.0, Instant.now(), Instant.now(), null, CompletionStrategy.MANUAL,
                List.of(), Map.of(), Instant.now(), Instant.now()
        ));

        GoalResponse response = goalService.completeGoal(userId, goalId);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(GoalStatus.COMPLETED);
        verify(eventPublisher).publishEvent(any(GoalCompletedEvent.class));
    }

    @Test
    void shouldCreateMilestoneSuccessfully() {
        UUID goalId = UUID.randomUUID();
        goal.setId(goalId);
        CreateMilestoneRequest request = new CreateMilestoneRequest("Milestone 1", "Description", 0);
        when(goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, userId)).thenReturn(Optional.of(goal));
        when(milestoneRepository.findByGoalIdAndDeletedAtIsNullOrderByDisplayOrderAsc(goalId)).thenReturn(List.of());
        when(milestoneRepository.save(any(GoalMilestone.class))).thenAnswer(invocation -> {
            GoalMilestone m = invocation.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });
        when(goalMapper.toMilestoneResponse(any(GoalMilestone.class))).thenReturn(new MilestoneResponse(
                UUID.randomUUID(), goalId, "Milestone 1", "Description", 0, false, null, Instant.now(), Instant.now()
        ));

        MilestoneResponse response = goalService.createMilestone(userId, goalId, request);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Milestone 1");
        verify(milestoneRepository).save(any(GoalMilestone.class));
    }
}
