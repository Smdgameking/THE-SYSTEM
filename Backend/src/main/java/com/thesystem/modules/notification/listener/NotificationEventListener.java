package com.thesystem.modules.notification.listener;

import com.thesystem.modules.goal.events.GoalCompletedEvent;
import com.thesystem.modules.notification.service.NotificationService;
import com.thesystem.modules.task.events.TaskCompletedEvent;
import com.thesystem.modules.xp.events.AchievementUnlockedEvent;
import com.thesystem.modules.xp.events.LevelUpEvent;
import com.thesystem.modules.xp.events.StreakMilestoneReachedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class NotificationEventListener {

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @EventListener
    public void handleTaskCompleted(TaskCompletedEvent event) {
        if (event == null || event.userId() == null) {
            return;
        }
        notificationService.createNotification(
                event.userId(),
                "TASK_COMPLETED",
                "Task completed",
                event.title() + " completed",
                "TASK",
                event.taskId(),
                null
        );
    }

    @EventListener
    public void handleGoalCompleted(GoalCompletedEvent event) {
        if (event == null || event.userId() == null) {
            return;
        }
        notificationService.createNotification(
                event.userId(),
                "GOAL_COMPLETED",
                "Goal completed",
                (event.difficulty() != null ? event.difficulty().toLowerCase() : "Goal") + " goal completed",
                "GOAL",
                event.goalId(),
                null
        );
    }

    @EventListener
    public void handleAchievementUnlocked(AchievementUnlockedEvent event) {
        if (event == null || event.userId() == null) {
            return;
        }
        notificationService.createNotification(
                event.userId(),
                "ACHIEVEMENT_UNLOCKED",
                "Achievement unlocked",
                event.achievementCode() + " unlocked (+" + event.xpReward() + " XP)",
                "ACHIEVEMENT",
                event.achievementId(),
                null
        );
    }

    @EventListener
    public void handleLevelUp(LevelUpEvent event) {
        if (event == null || event.userId() == null) {
            return;
        }
        notificationService.createNotification(
                event.userId(),
                "LEVEL_UP",
                "Level up!",
                "You reached level " + event.newLevel(),
                "LEVEL",
                null,
                null
        );
    }

    @EventListener
    public void handleStreakMilestoneReached(StreakMilestoneReachedEvent event) {
        if (event == null || event.userId() == null) {
            return;
        }
        notificationService.createNotification(
                event.userId(),
                "STREAK_MILESTONE",
                "Streak milestone!",
                event.milestone() + "-day streak reached",
                "STREAK",
                null,
                null
        );
    }
}
