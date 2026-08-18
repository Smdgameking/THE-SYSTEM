package com.thesystem.modules.goal.listener;

import com.thesystem.modules.goal.service.GoalService;
import com.thesystem.modules.task.events.TaskCompletedEvent;
import com.thesystem.modules.task.events.TaskCreatedEvent;
import com.thesystem.modules.task.events.TaskDeletedEvent;
import com.thesystem.modules.task.events.TaskUpdatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class GoalTaskProgressListener {

    private final GoalService goalService;

    public GoalTaskProgressListener(GoalService goalService) {
        this.goalService = goalService;
    }

    @EventListener
    public void onTaskCreated(TaskCreatedEvent event) {
        goalService.recalculateTaskBasedProgress(event.userId(), event.goalId());
    }

    @EventListener
    public void onTaskUpdated(TaskUpdatedEvent event) {
        goalService.recalculateTaskBasedProgress(event.userId(), event.goalId());
        if (event.previousGoalId() != null && !event.previousGoalId().equals(event.goalId())) {
            goalService.recalculateTaskBasedProgress(event.userId(), event.previousGoalId());
        }
    }

    @EventListener
    public void onTaskDeleted(TaskDeletedEvent event) {
        goalService.recalculateTaskBasedProgress(event.userId(), event.goalId());
    }

    @EventListener
    public void onTaskCompleted(TaskCompletedEvent event) {
        goalService.recalculateTaskBasedProgress(event.userId(), event.goalId());
    }
}
