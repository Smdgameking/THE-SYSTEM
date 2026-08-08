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
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class XpEventListenerTest {

    @Mock
    private XpService xpService;

    private XpEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new XpEventListener(xpService);
    }

    @Test
    void shouldAwardXpForTaskCompletion() {
        Long taskId = 123L;
        Long userId = 456L;
        TaskCompletedEvent event = new TaskCompletedEvent(taskId, userId, null, "Test Task", "MANUAL");

        listener.handleTaskCompleted(event);

        verify(xpService).createTransaction(any(UUID.class), any(TransactionCreateRequest.class));
    }

    @Test
    void shouldAwardXpForGoalCompletion() {
        UUID goalId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        GoalCompletedEvent event = new GoalCompletedEvent(goalId, userId, 100);

        listener.handleGoalCompleted(event);

        verify(xpService).createTransaction(eq(userId), any(TransactionCreateRequest.class));
    }

    @Test
    void shouldUseDefaultXpWhenGoalEstimatedXpIsZero() {
        UUID goalId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        GoalCompletedEvent event = new GoalCompletedEvent(goalId, userId, 0);

        listener.handleGoalCompleted(event);

        verify(xpService).createTransaction(eq(userId), any(TransactionCreateRequest.class));
    }
}
