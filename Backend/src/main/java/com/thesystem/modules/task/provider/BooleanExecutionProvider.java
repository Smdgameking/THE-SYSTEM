package com.thesystem.modules.task.provider;

import com.thesystem.modules.task.entity.Task;
import com.thesystem.modules.task.enums.TaskExecutionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class BooleanExecutionProvider implements TaskExecutionProvider {

    @Override
    public TaskExecutionType getType() {
        return TaskExecutionType.BOOLEAN;
    }

    @Override
    public TaskExecutionState initialize(Task task) {
        return new TaskExecutionState("{\"value\": false}", new ObjectMapper());
    }

    @Override
    public TaskExecutionState calculateProgress(Task task, TaskExecutionState state) {
        Boolean value = state.get("value");
        if (value == null) {
            value = false;
        }
        int progress = value ? 100 : 0;
        state.put("progress", progress);
        return state;
    }

    @Override
    public boolean isComplete(Task task, TaskExecutionState state) {
        Boolean value = state.get("value");
        return value != null && value;
    }

    @Override
    public void validate(Task task, TaskExecutionState state) {
        if (!state.getData().containsKey("value")) {
            throw new com.thesystem.common.exception.BusinessException(
                    com.thesystem.common.constants.ErrorCodes.VALIDATION_ERROR,
                    "Boolean execution state must contain 'value' field"
            );
        }
        Object value = state.get("value");
        if (!(value instanceof Boolean)) {
            throw new com.thesystem.common.exception.BusinessException(
                    com.thesystem.common.constants.ErrorCodes.VALIDATION_ERROR,
                    "Boolean execution state 'value' must be a boolean"
            );
        }
    }
}
