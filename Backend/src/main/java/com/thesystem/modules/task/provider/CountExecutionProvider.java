package com.thesystem.modules.task.provider;

import com.thesystem.modules.task.entity.Task;
import com.thesystem.modules.task.enums.TaskExecutionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class CountExecutionProvider implements TaskExecutionProvider {

    @Override
    public TaskExecutionType getType() {
        return TaskExecutionType.COUNT;
    }

    @Override
    public TaskExecutionState initialize(Task task) {
        return new TaskExecutionState("{\"current\": 0, \"target\": 1}", new ObjectMapper());
    }

    @Override
    public TaskExecutionState calculateProgress(Task task, TaskExecutionState state) {
        Integer current = state.get("current");
        if (current == null) {
            current = 0;
        }
        Integer target = state.get("target");
        if (target == null || target <= 0) {
            state.put("progress", 0);
            return state;
        }
        int progress = Math.min(100, (int) (((double) current / target) * 100));
        state.put("progress", progress);
        return state;
    }

    @Override
    public boolean isComplete(Task task, TaskExecutionState state) {
        Integer current = state.get("current");
        Integer target = state.get("target");
        if (current == null || target == null || target <= 0) {
            return false;
        }
        return current >= target;
    }

    @Override
    public void validate(Task task, TaskExecutionState state) {
        Integer current = state.get("current");
        Integer target = state.get("target");
        if (current != null && current < 0) {
            throw new com.thesystem.common.exception.BusinessException(
                    com.thesystem.common.constants.ErrorCodes.VALIDATION_ERROR,
                    "Count current cannot be negative"
            );
        }
        if (target != null && target <= 0) {
            throw new com.thesystem.common.exception.BusinessException(
                    com.thesystem.common.constants.ErrorCodes.VALIDATION_ERROR,
                    "Count target must be greater than 0"
            );
        }
    }
}
