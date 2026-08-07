package com.thesystem.modules.task.provider;

import com.thesystem.modules.task.entity.Task;
import com.thesystem.modules.task.enums.TaskExecutionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ProgressExecutionProvider implements TaskExecutionProvider {

    @Override
    public TaskExecutionType getType() {
        return TaskExecutionType.PROGRESS;
    }

    @Override
    public TaskExecutionState initialize(Task task) {
        return new TaskExecutionState("{\"percentage\": 0}", new ObjectMapper());
    }

    @Override
    public TaskExecutionState calculateProgress(Task task, TaskExecutionState state) {
        Object percentage = state.get("percentage");
        int value = 0;
        if (percentage instanceof Number) {
            value = ((Number) percentage).intValue();
        }
        value = Math.max(0, Math.min(100, value));
        state.put("progress", value);
        return state;
    }

    @Override
    public boolean isComplete(Task task, TaskExecutionState state) {
        Object percentage = state.get("percentage");
        if (percentage instanceof Number) {
            return ((Number) percentage).intValue() >= 100;
        }
        return false;
    }

    @Override
    public void validate(Task task, TaskExecutionState state) {
        if (!state.getData().containsKey("percentage")) {
            throw new com.thesystem.common.exception.BusinessException(
                    com.thesystem.common.constants.ErrorCodes.VALIDATION_ERROR,
                    "Progress execution state must contain 'percentage' field"
            );
        }
        Object percentage = state.get("percentage");
        if (!(percentage instanceof Number)) {
            throw new com.thesystem.common.exception.BusinessException(
                    com.thesystem.common.constants.ErrorCodes.VALIDATION_ERROR,
                    "Progress execution state 'percentage' must be a number"
            );
        }
    }
}
