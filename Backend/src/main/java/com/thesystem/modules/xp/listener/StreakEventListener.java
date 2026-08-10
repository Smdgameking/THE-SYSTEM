package com.thesystem.modules.xp.listener;

import com.thesystem.modules.goal.events.GoalCompletedEvent;
import com.thesystem.modules.task.events.TaskCompletedEvent;
import com.thesystem.modules.xp.service.impl.StreakEngine;
import org.springframework.core.annotation.Order;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class StreakEventListener {

    private final StreakEngine streakEngine;

    public StreakEventListener(StreakEngine streakEngine) {
        this.streakEngine = streakEngine;
    }

    @EventListener
    public void handleTaskCompleted(TaskCompletedEvent event) {
        streakEngine.handleTaskCompleted(event);
    }

    @EventListener
    public void handleGoalCompleted(GoalCompletedEvent event) {
        streakEngine.handleGoalCompleted(event);
    }
}
