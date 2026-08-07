package com.thesystem.modules.task.provider;

import com.thesystem.modules.task.entity.Task;
import com.thesystem.modules.task.enums.TaskExecutionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class CustomExecutionProvider implements TaskExecutionProvider {

    @Override
    public TaskExecutionType getType() {
        return TaskExecutionType.CUSTOM;
    }

    @Override
    public TaskExecutionState initialize(Task task) {
        return new TaskExecutionState("{\"data\": {}}", new ObjectMapper());
    }

    @Override
    public TaskExecutionState calculateProgress(Task task, TaskExecutionState state) {
        Object completed = state.get("completed");
        int progress = Boolean.TRUE.equals(completed) ? 100 : 0;
        state.put("progress", progress);
        return state;
    }

    @Override
    public boolean isComplete(Task task, TaskExecutionState state) {
        Object completed = state.get("completed");
        return Boolean.TRUE.equals(completed);
    }

    @Override
    public void validate(Task task, TaskExecutionState state) {
        if (!state.getData().containsKey("data")) {
            throw new com.thesystem.common.exception.BusinessException(
                    com.thesystem.common.constants.ErrorCodes.VALIDATION_ERROR,
                    "Custom execution state must contain 'data' field"
            );
        }
        Object data = state.get("data");
        if (!(data instanceof java.util.Map)) {
            throw new com.thesystem.common.exception.BusinessException(
                    com.thesystem.common.constants.ErrorCodes.VALIDATION_ERROR,
                    "Custom execution state 'data' must be a map"
            );
        }
    }
}
