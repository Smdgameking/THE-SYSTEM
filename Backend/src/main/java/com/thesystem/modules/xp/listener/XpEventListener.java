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
import org.springframework.core.annotation.Order;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Order(2)
public class XpEventListener {

    private static final Logger logger = LoggerFactory.getLogger(XpEventListener.class);

    private final XpService xpService;
    private final TaskRepository taskRepository;

    public XpEventListener(XpService xpService, TaskRepository taskRepository) {
        this.xpService = xpService;
        this.taskRepository = taskRepository;
    }

    @EventListener
    public void handleTaskCompleted(TaskCompletedEvent event) {
        try {
            UUID userId = event.userId();
            UUID taskId = event.taskId();

            String priority = "NORMAL";
            String difficulty = null;
            if (taskId != null && userId != null) {
                Task task = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId).orElse(null);
                if (task != null) {
                    if (task.getPriority() != null) {
                        priority = task.getPriority().name();
                    }
                    if (task.getDifficulty() != null) {
                        difficulty = task.getDifficulty().name();
                    }
                }
            }

            java.util.Map<String, Object> context = new java.util.HashMap<>();
            context.put("taskPriority", priority);
            if (difficulty != null) {
                context.put("taskDifficulty", difficulty);
            }
            context.put("executionType", event.executionType() != null ? event.executionType() : "UNKNOWN");

            XpService.XpCalculationResult calculation = xpService.calculateXpForEvent(userId, context, XpService.XpSourceType.TASK);

            TransactionCreateRequest request = new TransactionCreateRequest(
                    TransactionType.TASK_COMPLETION,
                    calculation.finalXp(),
                    "task-engine",
                    taskId,
                    "TASK",
                    "Task completed: " + event.title(),
                    context
            );

            xpService.createTransaction(userId, request, calculation.primaryPolicyId(), calculation.multiplier(), calculation.baseXp());
        } catch (Exception e) {
            logger.error("Failed to award XP for task completion: {}", event, e);
        }
    }

    @EventListener
    public void handleGoalCompleted(GoalCompletedEvent event) {
        try {
            UUID userId = event.userId();
            UUID goalId = event.goalId();

            java.util.Map<String, Object> context = new java.util.HashMap<>();
            if (event.difficulty() != null) {
                context.put("goalDifficulty", event.difficulty());
            }
            context.put("goalEstimatedXp", event.estimatedXp());

            XpService.XpCalculationResult calculation = xpService.calculateXpForEvent(userId, context, XpService.XpSourceType.GOAL);

            TransactionCreateRequest request = new TransactionCreateRequest(
                    TransactionType.GOAL_COMPLETION,
                    calculation.finalXp(),
                    "goal-engine",
                    goalId,
                    "GOAL",
                    "Goal completed",
                    context
            );

            xpService.createTransaction(userId, request, calculation.primaryPolicyId(), calculation.multiplier(), calculation.baseXp());
        } catch (Exception e) {
            logger.error("Failed to award XP for goal completion: {}", event, e);
        }
    }

}
