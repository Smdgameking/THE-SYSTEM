package com.thesystem.modules.xp.listener;

import com.thesystem.modules.goal.events.GoalCompletedEvent;
import com.thesystem.modules.task.events.TaskCompletedEvent;
import com.thesystem.modules.xp.listener.XpEventListener;
import com.thesystem.modules.xp.service.impl.StreakEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.Order;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StreakEventListenerTest {

    @Mock
    private StreakEngine streakEngine;

    private StreakEventListener listener;

    @Test
    void shouldDelegateTaskCompletedToStreakEngine() {
        listener = new StreakEventListener(streakEngine);
        UUID taskId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        TaskCompletedEvent event = new TaskCompletedEvent(taskId, userId, null, "Task", "MANUAL", "NORMAL", Instant.parse("2026-08-01T10:00:00Z"));

        listener.handleTaskCompleted(event);

        verify(streakEngine).handleTaskCompleted(event);
    }

    @Test
    void shouldDelegateGoalCompletedToStreakEngine() {
        listener = new StreakEventListener(streakEngine);
        UUID goalId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        GoalCompletedEvent event = new GoalCompletedEvent(goalId, userId, 100, "NORMAL", Instant.parse("2026-08-01T10:00:00Z"));

        listener.handleGoalCompleted(event);

        verify(streakEngine).handleGoalCompleted(event);
    }

    @Test
    void streakEventListenerShouldHaveLowerOrderThanXpEventListener() {
        Order streakOrder = StreakEventListener.class.getAnnotation(Order.class);
        Order xpOrder = XpEventListener.class.getAnnotation(Order.class);

        assertThat(streakOrder).isNotNull();
        assertThat(xpOrder).isNotNull();
        assertThat(streakOrder.value()).isLessThan(xpOrder.value());
    }
}
