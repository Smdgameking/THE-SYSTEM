package com.thesystem.modules.task.provider;

import com.thesystem.modules.task.entity.Task;
import com.thesystem.modules.task.enums.TaskExecutionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class HabitExecutionProvider implements TaskExecutionProvider {

    @Override
    public TaskExecutionType getType() {
        return TaskExecutionType.HABIT;
    }

    @Override
    public TaskExecutionState initialize(Task task) {
        return new TaskExecutionState("{\"streak\": 0, \"lastCompletedDate\": null}", new ObjectMapper());
    }

    @Override
    public TaskExecutionState calculateProgress(Task task, TaskExecutionState state) {
        Integer streak = state.get("streak");
        int progress = (streak != null && streak > 0) ? 100 : 0;
        state.put("progress", progress);
        return state;
    }

    @Override
    public boolean isComplete(Task task, TaskExecutionState state) {
        Integer streak = state.get("streak");
        return streak != null && streak > 0;
    }

    @Override
    public void validate(Task task, TaskExecutionState state) {
        if (!state.getData().containsKey("streak")) {
            throw new com.thesystem.common.exception.BusinessException(
                    com.thesystem.common.constants.ErrorCodes.VALIDATION_ERROR,
                    "Habit execution state must contain 'streak' field"
            );
        }
        Object streak = state.get("streak");
        if (!(streak instanceof Integer)) {
            throw new com.thesystem.common.exception.BusinessException(
                    com.thesystem.common.constants.ErrorCodes.VALIDATION_ERROR,
                    "Habit execution state 'streak' must be an integer"
            );
        }
        if ((Integer) streak < 0) {
            throw new com.thesystem.common.exception.BusinessException(
                    com.thesystem.common.constants.ErrorCodes.VALIDATION_ERROR,
                    "Habit streak cannot be negative"
            );
        }
    }
}
