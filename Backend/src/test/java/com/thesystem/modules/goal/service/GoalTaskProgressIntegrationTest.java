package com.thesystem.modules.goal.service;

import com.thesystem.modules.goal.dto.CreateGoalRequest;
import com.thesystem.modules.goal.dto.GoalResponse;
import com.thesystem.modules.goal.entity.Goal;
import com.thesystem.modules.goal.enums.CompletionStrategy;
import com.thesystem.modules.goal.enums.GoalDifficulty;
import com.thesystem.modules.goal.enums.GoalPriority;
import com.thesystem.modules.goal.enums.GoalStatus;
import com.thesystem.modules.goal.enums.GoalType;
import com.thesystem.modules.goal.enums.GoalVisibility;
import com.thesystem.modules.goal.repository.GoalRepository;
import com.thesystem.modules.task.dto.CreateTaskRequest;
import com.thesystem.modules.task.dto.UpdateTaskRequest;
import com.thesystem.modules.task.enums.TaskStatus;
import com.thesystem.modules.task.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:goaltaskprogress;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@Transactional
class GoalTaskProgressIntegrationTest {

    @Autowired
    private GoalService goalService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private GoalRepository goalRepository;

    private CreateGoalRequest goalRequest(String title) {
        return new CreateGoalRequest(
                title, "Description", "integration",
                GoalPriority.NORMAL, GoalDifficulty.NORMAL, GoalType.PROJECT,
                GoalVisibility.PRIVATE, 100, null, CompletionStrategy.TASK_BASED, null, null
        );
    }

    private CreateTaskRequest taskRequest(UUID goalId, String title) {
        return new CreateTaskRequest(
                title, null, goalId, null, TaskStatus.IN_PROGRESS, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null
        );
    }

    private UpdateTaskRequest statusRequest(TaskStatus status) {
        return new UpdateTaskRequest(
                null, null, null, null, status, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null
        );
    }

    private UpdateTaskRequest goalMoveRequest(UUID goalId) {
        return new UpdateTaskRequest(
                null, null, goalId, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null
        );
    }

    private Goal load(UUID goalId, UUID userId) {
        return goalRepository.findByIdAndUserIdAndDeletedAtIsNull(goalId, userId).orElseThrow();
    }

    @Test
    void shouldDriveTaskBasedGoalProgressToCompletion() {
        UUID userId = UUID.randomUUID();
        GoalResponse created = goalService.createGoal(userId, goalRequest("Project"));
        UUID goalId = created.id();

        taskService.createTask(userId, taskRequest(goalId, "Task 1"));
        taskService.createTask(userId, taskRequest(goalId, "Task 2"));
        taskService.createTask(userId, taskRequest(goalId, "Task 3"));

        assertThat(load(goalId, userId).getCurrentProgress()).isZero();

        var tasks = taskService.listTasks(userId, new com.thesystem.modules.task.dto.TaskFilterRequest(
                null, null, null, goalId, null, null, null, null, null, null, null, null, null, null));
        taskService.completeTask(userId, tasks.get(0).id());
        taskService.completeTask(userId, tasks.get(1).id());

        Goal afterTwo = load(goalId, userId);
        assertThat(afterTwo.getCurrentProgress()).isEqualTo(67);
        assertThat(afterTwo.getStatus()).isEqualTo(GoalStatus.DRAFT);

        taskService.completeTask(userId, tasks.get(2).id());

        Goal completed = load(goalId, userId);
        assertThat(completed.getCurrentProgress()).isEqualTo(100);
        assertThat(completed.getCompletionPercentage()).isEqualTo(100.0);
        assertThat(completed.getStatus()).isEqualTo(GoalStatus.COMPLETED);
        assertThat(completed.getCompletedDate()).isNotNull();
    }

    @Test
    void shouldRevertCompletedGoalWhenTaskIsUncompleted() {
        UUID userId = UUID.randomUUID();
        GoalResponse created = goalService.createGoal(userId, goalRequest("Regression"));
        UUID goalId = created.id();

        var tasks = new java.util.ArrayList<UUID>();
        tasks.add(taskService.createTask(userId, taskRequest(goalId, "Task 1")).id());
        tasks.add(taskService.createTask(userId, taskRequest(goalId, "Task 2")).id());

        taskService.completeTask(userId, tasks.get(0));
        taskService.completeTask(userId, tasks.get(1));

        assertThat(load(goalId, userId).getStatus()).isEqualTo(GoalStatus.COMPLETED);

        taskService.updateTask(userId, tasks.get(0), statusRequest(TaskStatus.DRAFT));

        Goal regressed = load(goalId, userId);
        assertThat(regressed.getCurrentProgress()).isEqualTo(50);
        assertThat(regressed.getStatus()).isEqualTo(GoalStatus.ACTIVE);
        assertThat(regressed.getCompletedDate()).isNull();
    }

    @Test
    void shouldRegressProgressWhenLinkedTaskIsDeleted() {
        UUID userId = UUID.randomUUID();
        GoalResponse created = goalService.createGoal(userId, goalRequest("Deletion"));
        UUID goalId = created.id();

        UUID task1 = taskService.createTask(userId, taskRequest(goalId, "Task 1")).id();
        taskService.createTask(userId, taskRequest(goalId, "Task 2"));

        taskService.completeTask(userId, task1);

        assertThat(load(goalId, userId).getCurrentProgress()).isEqualTo(50);

        taskService.deleteTask(userId, task1);

        assertThat(load(goalId, userId).getCurrentProgress()).isZero();
        assertThat(load(goalId, userId).getStatus()).isEqualTo(GoalStatus.DRAFT);
    }

    @Test
    void shouldRecalculateBothGoalsWhenTaskMoves() {
        UUID userId = UUID.randomUUID();
        GoalResponse goalA = goalService.createGoal(userId, goalRequest("Goal A"));
        GoalResponse goalB = goalService.createGoal(userId, goalRequest("Goal B"));

        UUID taskA = taskService.createTask(userId, taskRequest(goalA.id(), "A1")).id();
        UUID taskB = taskService.createTask(userId, taskRequest(goalB.id(), "B1")).id();

        taskService.completeTask(userId, taskA);
        assertThat(load(goalA.id(), userId).getCurrentProgress()).isEqualTo(100);

        taskService.updateTask(userId, taskB, goalMoveRequest(goalA.id()));

        assertThat(load(goalA.id(), userId).getCurrentProgress()).isEqualTo(50);
        assertThat(load(goalB.id(), userId).getCurrentProgress()).isZero();
    }

    @Test
    void shouldNotAffectAnotherUsersGoal() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        GoalResponse goalA = goalService.createGoal(userA, goalRequest("A Goal"));
        UUID taskA = taskService.createTask(userA, taskRequest(goalA.id(), "A1")).id();

        GoalResponse goalB = goalService.createGoal(userB, goalRequest("B Goal"));
        taskService.createTask(userB, taskRequest(goalB.id(), "B1"));

        taskService.completeTask(userA, taskA);

        assertThat(load(goalA.id(), userA).getCurrentProgress()).isEqualTo(100);
        assertThat(load(goalB.id(), userB).getCurrentProgress()).isZero();
    }
}
