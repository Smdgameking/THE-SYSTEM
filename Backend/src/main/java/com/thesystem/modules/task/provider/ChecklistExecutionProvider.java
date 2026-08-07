package com.thesystem.modules.task.provider;

import com.thesystem.modules.task.entity.Task;
import com.thesystem.modules.task.enums.TaskExecutionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ChecklistExecutionProvider implements TaskExecutionProvider {

    @Override
    public TaskExecutionType getType() {
        return TaskExecutionType.CHECKLIST;
    }

    @Override
    public TaskExecutionState initialize(Task task) {
        return new TaskExecutionState("{\"items\": [], \"completedCount\": 0}", new ObjectMapper());
    }

    @Override
    public TaskExecutionState calculateProgress(Task task, TaskExecutionState state) {
        java.util.List<java.util.Map<String, Object>> items = state.get("items");
        if (items == null || items.isEmpty()) {
            state.put("progress", 0);
            return state;
        }
        long completed = items.stream()
                .filter(item -> Boolean.TRUE.equals(item.get("completed")))
                .count();
        int progress = (int) ((completed * 100) / items.size());
        state.put("completedCount", (int) completed);
        state.put("progress", progress);
        return state;
    }

    @Override
    public boolean isComplete(Task task, TaskExecutionState state) {
        java.util.List<java.util.Map<String, Object>> items = state.get("items");
        if (items == null || items.isEmpty()) {
            return false;
        }
        return items.stream()
                .allMatch(item -> Boolean.TRUE.equals(item.get("completed")));
    }

    @Override
    public void validate(Task task, TaskExecutionState state) {
        if (!state.getData().containsKey("items")) {
            throw new com.thesystem.common.exception.BusinessException(
                    com.thesystem.common.constants.ErrorCodes.VALIDATION_ERROR,
                    "Checklist execution state must contain 'items' field"
            );
        }
        Object items = state.get("items");
        if (!(items instanceof java.util.List)) {
            throw new com.thesystem.common.exception.BusinessException(
                    com.thesystem.common.constants.ErrorCodes.VALIDATION_ERROR,
                    "Checklist execution state 'items' must be a list"
            );
        }
    }
}
