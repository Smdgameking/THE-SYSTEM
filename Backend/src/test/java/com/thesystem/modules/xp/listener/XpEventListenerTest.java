package com.thesystem.modules.xp.listener;

import com.thesystem.modules.goal.events.GoalCompletedEvent;
import com.thesystem.modules.task.events.TaskCompletedEvent;
import com.thesystem.modules.xp.dto.transaction.TransactionCreateRequest;
import com.thesystem.modules.xp.enums.TransactionType;
import com.thesystem.modules.xp.service.XpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class XpEventListenerTest {

    @Mock
    private XpService xpService;

    @Mock
    private com.thesystem.modules.task.repository.TaskRepository taskRepository;

    private XpEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new XpEventListener(xpService, taskRepository);
    }

    @Test
    void shouldAwardXpForTaskCompletionWithPolicyMultiplier() {
        Long taskId = 123L;
        Long userId = 456L;
        TaskCompletedEvent event = new TaskCompletedEvent(taskId, userId, null, "Test Task", "MANUAL", null);

        XpService.XpCalculationResult calculation = new XpService.XpCalculationResult(
                UUID.randomUUID(), 10, 1.5, 15);
        when(xpService.calculateXpForEvent(any(UUID.class), any(), eq(XpService.XpSourceType.TASK))).thenReturn(calculation);
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(any(UUID.class), any(UUID.class))).thenReturn(java.util.Optional.empty());

        listener.handleTaskCompleted(event);

        verify(xpService).calculateXpForEvent(any(UUID.class), any(), eq(XpService.XpSourceType.TASK));
        verify(xpService).createTransaction(any(UUID.class), any(TransactionCreateRequest.class), any(UUID.class), any(), any());
    }

    @Test
    void shouldApplyBaseXpWhenNoPoliciesMatch() {
        Long taskId = 123L;
        Long userId = 456L;
        TaskCompletedEvent event = new TaskCompletedEvent(taskId, userId, null, "Test Task", "MANUAL", null);

        XpService.XpCalculationResult calculation = new XpService.XpCalculationResult(
                UUID.randomUUID(), 10, 1.0, 10);
        when(xpService.calculateXpForEvent(any(UUID.class), any(), eq(XpService.XpSourceType.TASK))).thenReturn(calculation);
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(any(UUID.class), any(UUID.class))).thenReturn(java.util.Optional.empty());

        listener.handleTaskCompleted(event);

        verify(xpService).createTransaction(any(UUID.class), any(TransactionCreateRequest.class), any(UUID.class), any(), any());
    }

    @Test
    void shouldAwardXpForGoalCompletion() {
        UUID goalId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        GoalCompletedEvent event = new GoalCompletedEvent(goalId, userId, 100, "HARD");

        XpService.XpCalculationResult calculation = new XpService.XpCalculationResult(
                UUID.randomUUID(), 100, 1.0, 100);
        when(xpService.calculateXpForEvent(eq(userId), any(), eq(XpService.XpSourceType.GOAL))).thenReturn(calculation);

        listener.handleGoalCompleted(event);

        verify(xpService).calculateXpForEvent(eq(userId), any(), eq(XpService.XpSourceType.GOAL));
        verify(xpService).createTransaction(eq(userId), any(TransactionCreateRequest.class), any(UUID.class), any(), any());
    }

    @Test
    void shouldUseDefaultXpWhenGoalEstimatedXpIsZero() {
        UUID goalId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        GoalCompletedEvent event = new GoalCompletedEvent(goalId, userId, 0, "NORMAL");

        XpService.XpCalculationResult calculation = new XpService.XpCalculationResult(
                UUID.randomUUID(), 100, 1.0, 100);
        when(xpService.calculateXpForEvent(eq(userId), any(), eq(XpService.XpSourceType.GOAL))).thenReturn(calculation);

        listener.handleGoalCompleted(event);

        verify(xpService).createTransaction(eq(userId), any(TransactionCreateRequest.class), any(UUID.class), any(), any());
    }
}
