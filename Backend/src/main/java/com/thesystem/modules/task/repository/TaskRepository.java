package com.thesystem.modules.task.repository;

import com.thesystem.modules.task.entity.Task;
import com.thesystem.modules.task.enums.TaskExecutionType;
import com.thesystem.modules.task.enums.TaskPriority;
import com.thesystem.modules.task.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByUserIdAndDeletedAtIsNull(UUID userId);

    List<Task> findByUserIdAndStatusAndDeletedAtIsNull(UUID userId, TaskStatus status);

    List<Task> findByUserIdAndPriorityAndDeletedAtIsNull(UUID userId, TaskPriority priority);

    List<Task> findByUserIdAndGoalIdAndDeletedAtIsNull(UUID userId, UUID goalId);

    List<Task> findByUserIdAndParentTaskIdAndDeletedAtIsNull(UUID userId, UUID parentTaskId);

    List<Task> findByUserIdAndExecutionTypeAndDeletedAtIsNull(UUID userId, TaskExecutionType executionType);

    List<Task> findByUserIdAndIsRecurringAndDeletedAtIsNull(UUID userId, Boolean isRecurring);

    Optional<Task> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    boolean existsByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    List<Task> findByUserIdAndDueDateBeforeAndDeletedAtIsNullAndStatusNotIn(UUID userId, Instant dueDate, List<TaskStatus> excludedStatuses);

    List<Task> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID userId);

    long countByUserIdAndDeletedAtIsNull(UUID userId);

    long countByUserIdAndStatusAndDeletedAtIsNull(UUID userId, TaskStatus status);
}
