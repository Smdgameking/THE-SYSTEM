package com.thesystem.modules.task.service;

import com.thesystem.common.exception.BusinessException;
import com.thesystem.common.constants.ErrorCodes;
import com.thesystem.modules.task.entity.Task;
import com.thesystem.modules.task.enums.TaskExecutionType;
import com.thesystem.modules.task.provider.TaskExecutionProviderRegistry;
import com.thesystem.modules.task.provider.TaskExecutionState;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class ExecutionStateValidation {

    public static TaskExecutionState validateExecutionState(Task task, TaskExecutionProviderRegistry registry) {
        if (task.getExecutionType() == null) {
            return new TaskExecutionState("{}", new ObjectMapper());
        }
        TaskExecutionState state = new TaskExecutionState(task.getExecutionState(), new ObjectMapper());
        registry.getProvider(task.getExecutionType()).validate(task, state);
        return state;
    }

    public static String serializeState(TaskExecutionState state) {
        return state.toJson();
    }

    public static TaskExecutionState deserializeState(String json, ObjectMapper objectMapper) {
        return new TaskExecutionState(json, objectMapper);
    }

    public static boolean isValidExecutionType(TaskExecutionType type) {
        return type != null;
    }

    public static Map<String, Object> getStateData(TaskExecutionState state) {
        return state.getData();
    }

    public static void validateStateNotNull(TaskExecutionState state) {
        if (state == null) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Execution state cannot be null");
        }
    }
}
