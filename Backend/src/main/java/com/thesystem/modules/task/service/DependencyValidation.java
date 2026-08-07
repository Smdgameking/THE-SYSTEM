package com.thesystem.modules.task.service;

import com.thesystem.common.exception.BusinessException;
import com.thesystem.common.constants.ErrorCodes;
import com.thesystem.modules.task.entity.Task;
import com.thesystem.modules.task.entity.TaskDependency;
import com.thesystem.modules.task.enums.DependencyType;

import java.util.*;

public class DependencyValidation {

    public static void validateNoSelfDependency(UUID taskId, UUID dependsOnTaskId) {
        if (taskId.equals(dependsOnTaskId)) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Task cannot depend on itself");
        }
    }

    public static void validateNoDuplicate(UUID taskId, UUID dependsOnTaskId, List<TaskDependency> existingDependencies) {
        for (TaskDependency dep : existingDependencies) {
            if (dep.getDependsOnTaskId().equals(dependsOnTaskId)) {
                throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Dependency already exists");
            }
        }
    }

    public static void validateNoCycle(UUID taskId, UUID dependsOnTaskId, Map<UUID, List<UUID>> dependencyGraph) {
        if (wouldCreateCycle(taskId, dependsOnTaskId, dependencyGraph)) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Dependency would create a cycle");
        }
    }

    public static boolean wouldCreateCycle(UUID taskId, UUID dependsOnTaskId, Map<UUID, List<UUID>> dependencyGraph) {
        if (taskId.equals(dependsOnTaskId)) {
            return true;
        }
        Set<UUID> visited = new HashSet<>();
        Deque<UUID> stack = new ArrayDeque<>();
        stack.push(dependsOnTaskId);
        while (!stack.isEmpty()) {
            UUID current = stack.pop();
            if (current.equals(taskId)) {
                return true;
            }
            if (!visited.add(current)) {
                continue;
            }
            List<UUID> neighbors = dependencyGraph.getOrDefault(current, List.of());
            for (UUID neighbor : neighbors) {
                stack.push(neighbor);
            }
        }
        return false;
    }

    public static void validateDependencyType(DependencyType type) {
        if (type == null) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Dependency type cannot be null");
        }
    }

    public static void validateDependencyTasksExist(Task task, Task dependsOnTask) {
        if (task == null || dependsOnTask == null) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Both tasks must exist");
        }
    }
}
