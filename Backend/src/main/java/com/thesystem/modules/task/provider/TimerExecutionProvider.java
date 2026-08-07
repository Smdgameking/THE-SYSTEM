package com.thesystem.modules.task.provider;

import com.thesystem.modules.task.entity.Task;
import com.thesystem.modules.task.enums.TaskExecutionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class TimerExecutionProvider implements TaskExecutionProvider {

    @Override
    public TaskExecutionType getType() {
        return TaskExecutionType.TIMER;
    }

    @Override
    public TaskExecutionState initialize(Task task) {
        return new TaskExecutionState("{\"elapsedSeconds\": 0, \"targetSeconds\": null}", new ObjectMapper());
    }

    @Override
    public TaskExecutionState calculateProgress(Task task, TaskExecutionState state) {
        Integer elapsed = state.get("elapsedSeconds");
        if (elapsed == null) {
            elapsed = 0;
        }
        Integer target = state.get("targetSeconds");
        if (target == null || target <= 0) {
            state.put("progress", 0);
            return state;
        }
        int progress = Math.min(100, (int) (((double) elapsed / target) * 100));
        state.put("progress", progress);
        return state;
    }

    @Override
    public boolean isComplete(Task task, TaskExecutionState state) {
        Integer elapsed = state.get("elapsedSeconds");
        Integer target = state.get("targetSeconds");
        if (elapsed == null || target == null || target <= 0) {
            return false;
        }
        return elapsed >= target;
    }

    @Override
    public void validate(Task task, TaskExecutionState state) {
        Integer elapsed = state.get("elapsedSeconds");
        Integer target = state.get("targetSeconds");
        if (elapsed != null && elapsed < 0) {
            throw new com.thesystem.common.exception.BusinessException(
                    com.thesystem.common.constants.ErrorCodes.VALIDATION_ERROR,
                    "Timer elapsedSeconds cannot be negative"
            );
        }
        if (target != null && target <= 0) {
            throw new com.thesystem.common.exception.BusinessException(
                    com.thesystem.common.constants.ErrorCodes.VALIDATION_ERROR,
                    "Timer targetSeconds must be greater than 0"
            );
        }
    }
}
