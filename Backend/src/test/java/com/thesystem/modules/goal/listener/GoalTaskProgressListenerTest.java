package com.thesystem.modules.goal.listener;

import com.thesystem.modules.goal.service.GoalService;
import com.thesystem.modules.task.events.TaskCompletedEvent;
import com.thesystem.modules.task.events.TaskCreatedEvent;
import com.thesystem.modules.task.events.TaskDeletedEvent;
import com.thesystem.modules.task.events.TaskUpdatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GoalTaskProgressListenerTest {

    @Mock
    private GoalService goalService;

    private GoalTaskProgressListener listener;

    @BeforeEach
    void setUp() {
        listener = new GoalTaskProgressListener(goalService);
    }

    @Test
    void shouldRecalculateGoalOnTaskCreated() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();

        listener.onTaskCreated(new TaskCreatedEvent(UUID.randomUUID(), userId, goalId, "Task"));

        verify(goalService).recalculateTaskBasedProgress(userId, goalId);
    }

    @Test
    void shouldRecalculateGoalOnceOnTaskUpdatedWithSameGoal() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();

        listener.onTaskUpdated(new TaskUpdatedEvent(UUID.randomUUID(), userId, goalId, goalId, "Task"));

        verify(goalService).recalculateTaskBasedProgress(userId, goalId);
        verify(goalService, never()).recalculateTaskBasedProgress(userId, UUID.randomUUID());
    }

    @Test
    void shouldRecalculateBothGoalsWhenTaskMoves() {
        UUID userId = UUID.randomUUID();
        UUID previousGoalId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();

        listener.onTaskUpdated(new TaskUpdatedEvent(UUID.randomUUID(), userId, goalId, previousGoalId, "Task"));

        verify(goalService).recalculateTaskBasedProgress(userId, goalId);
        verify(goalService).recalculateTaskBasedProgress(userId, previousGoalId);
    }

    @Test
    void shouldNotRecalculatePreviousGoalWhenItWasNull() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();

        listener.onTaskUpdated(new TaskUpdatedEvent(UUID.randomUUID(), userId, goalId, null, "Task"));

        verify(goalService).recalculateTaskBasedProgress(userId, goalId);
        verify(goalService, never()).recalculateTaskBasedProgress(userId, UUID.randomUUID());
    }

    @Test
    void shouldRecalculateGoalOnTaskDeleted() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();

        listener.onTaskDeleted(new TaskDeletedEvent(UUID.randomUUID(), userId, goalId));

        verify(goalService).recalculateTaskBasedProgress(userId, goalId);
    }

    @Test
    void shouldRecalculateGoalOnTaskCompleted() {
        UUID userId = UUID.randomUUID();
        UUID goalId = UUID.randomUUID();

        listener.onTaskCompleted(new TaskCompletedEvent(UUID.randomUUID(), userId, goalId, "Task", "BOOLEAN", "EASY"));

        verify(goalService).recalculateTaskBasedProgress(userId, goalId);
    }
}
