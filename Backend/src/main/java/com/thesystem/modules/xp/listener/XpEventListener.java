package com.thesystem.modules.xp.listener;

import com.thesystem.modules.goal.events.GoalCompletedEvent;
import com.thesystem.modules.task.entity.Task;
import com.thesystem.modules.task.events.TaskCompletedEvent;
import com.thesystem.modules.task.repository.TaskRepository;
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
    private final TaskRepository taskRepository;

    public XpEventListener(XpService xpService, TaskRepository taskRepository) {
        this.xpService = xpService;
        this.taskRepository = taskRepository;
    }

    @EventListener
    public void handleTaskCompleted(TaskCompletedEvent event) {
        try {
            UUID userId = toUuid(event.userId());
            UUID taskId = toUuid(event.taskId());

            String priority = "NORMAL";
            if (taskId != null && userId != null) {
                Task task = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId).orElse(null);
                if (task != null && task.getPriority() != null) {
                    priority = task.getPriority().name();
                }
            }

            Map<String, Object> context = Map.of(
                    "taskPriority", priority,
                    "executionType", event.executionType() != null ? event.executionType() : "UNKNOWN"
            );

            double multiplier = xpService.calculatePolicyMultiplier(userId, context);
            int finalXp = (int) Math.round(TASK_COMPLETION_BASE_XP * multiplier);

            TransactionCreateRequest request = new TransactionCreateRequest(
                    TransactionType.TASK_COMPLETION,
                    finalXp,
                    "task-engine",
                    taskId,
                    "TASK",
                    "Task completed: " + event.title(),
                    context
            );

            xpService.createTransaction(userId, request);
        } catch (Exception e) {
            logger.error("Failed to award XP for task completion: {}", event, e);
        }
    }

    @EventListener
    public void handleGoalCompleted(GoalCompletedEvent event) {
        try {
            int baseXp = event.estimatedXp() > 0 ? event.estimatedXp() : 100;

            Map<String, Object> context = Map.of(
                    "goalDifficulty", "NORMAL"
            );

            double multiplier = xpService.calculatePolicyMultiplier(event.userId(), context);
            int finalXp = (int) Math.round(baseXp * multiplier);

            TransactionCreateRequest request = new TransactionCreateRequest(
                    TransactionType.GOAL_COMPLETION,
                    finalXp,
                    "goal-engine",
                    event.goalId(),
                    "GOAL",
                    "Goal completed",
                    context
            );

            xpService.createTransaction(event.userId(), request);
        } catch (Exception e) {
            logger.error("Failed to award XP for goal completion: {}", event, e);
        }
    }

    private UUID toUuid(Long value) {
        if (value == null) {
            return null;
        }
        return UUID.nameUUIDFromBytes(String.valueOf(value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
