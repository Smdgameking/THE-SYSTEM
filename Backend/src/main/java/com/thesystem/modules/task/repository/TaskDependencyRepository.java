package com.thesystem.modules.task.repository;

import com.thesystem.modules.task.entity.TaskDependency;
import com.thesystem.modules.task.enums.DependencyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskDependencyRepository extends JpaRepository<TaskDependency, UUID> {

    List<TaskDependency> findByTaskIdAndDeletedAtIsNull(UUID taskId);

    List<TaskDependency> findByDependsOnTaskIdAndDeletedAtIsNull(UUID dependsOnTaskId);

    Optional<TaskDependency> findByTaskIdAndDependsOnTaskIdAndDeletedAtIsNull(UUID taskId, UUID dependsOnTaskId);

    boolean existsByTaskIdAndDependsOnTaskIdAndDeletedAtIsNull(UUID taskId, UUID dependsOnTaskId);

    List<TaskDependency> findByTaskIdAndStatusAndDeletedAtIsNull(UUID taskId, DependencyStatus status);

    List<TaskDependency> findByDependsOnTaskIdAndStatusAndDeletedAtIsNull(UUID dependsOnTaskId, DependencyStatus status);
}
