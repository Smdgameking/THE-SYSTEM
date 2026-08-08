package com.thesystem.modules.xp.listener;

import com.thesystem.modules.goal.events.GoalCompletedEvent;
import com.thesystem.modules.task.events.TaskCompletedEvent;
import com.thesystem.modules.xp.dto.transaction.TransactionCreateRequest;
import com.thesystem.modules.xp.enums.TransactionType;
import com.thesystem.modules.xp.service.XpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class XpEventListener {

    private static final Logger logger = LoggerFactory.getLogger(XpEventListener.class);
    private static final int TASK_COMPLETION_BASE_XP = 10;

    private final XpService xpService;

    public XpEventListener(XpService xpService) {
        this.xpService = xpService;
    }

    @EventListener
    public void handleTaskCompleted(TaskCompletedEvent event) {
        try {
            UUID userId = toUuid(event.userId());
            UUID taskId = toUuid(event.taskId());

            TransactionCreateRequest request = new TransactionCreateRequest(
                    TransactionType.TASK_COMPLETION,
                    TASK_COMPLETION_BASE_XP,
                    "task-engine",
                    taskId,
                    "TASK",
                    "Task completed: " + event.title(),
                    Map.of("executionType", event.executionType() != null ? event.executionType() : "UNKNOWN")
            );

            xpService.createTransaction(userId, request);
        } catch (Exception e) {
            logger.error("Failed to award XP for task completion: {}", event, e);
        }
    }

    private UUID toUuid(Long value) {
        if (value == null) {
            return null;
        }
        return UUID.nameUUIDFromBytes(String.valueOf(value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @EventListener
    public void handleGoalCompleted(GoalCompletedEvent event) {
        try {
            int baseXp = event.estimatedXp() > 0 ? event.estimatedXp() : 100;

            TransactionCreateRequest request = new TransactionCreateRequest(
                    TransactionType.GOAL_COMPLETION,
                    baseXp,
                    "goal-engine",
                    event.goalId(),
                    "GOAL",
                    "Goal completed",
                    Map.of()
            );

            xpService.createTransaction(event.userId(), request);
        } catch (Exception e) {
            logger.error("Failed to award XP for goal completion: {}", event, e);
        }
    }
}
