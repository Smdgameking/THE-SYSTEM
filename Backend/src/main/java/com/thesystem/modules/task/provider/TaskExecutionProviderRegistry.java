package com.thesystem.modules.task.provider;

import com.thesystem.modules.task.entity.Task;
import com.thesystem.modules.task.enums.TaskExecutionType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TaskExecutionProviderRegistry {

    private final Map<TaskExecutionType, TaskExecutionProvider> providers = new ConcurrentHashMap<>();

    public TaskExecutionProviderRegistry(
            BooleanExecutionProvider booleanProvider,
            ChecklistExecutionProvider checklistProvider,
            TimerExecutionProvider timerProvider,
            CountExecutionProvider countProvider,
            ProgressExecutionProvider progressProvider,
            HabitExecutionProvider habitProvider,
            ApprovalExecutionProvider approvalProvider,
            CustomExecutionProvider customProvider
    ) {
        providers.put(booleanProvider.getType(), booleanProvider);
        providers.put(checklistProvider.getType(), checklistProvider);
        providers.put(timerProvider.getType(), timerProvider);
        providers.put(countProvider.getType(), countProvider);
        providers.put(progressProvider.getType(), progressProvider);
        providers.put(habitProvider.getType(), habitProvider);
        providers.put(approvalProvider.getType(), approvalProvider);
        providers.put(customProvider.getType(), customProvider);
    }

    public TaskExecutionProvider getProvider(TaskExecutionType type) {
        if (type == null) {
            throw new com.thesystem.common.exception.BusinessException(
                    com.thesystem.common.constants.ErrorCodes.VALIDATION_ERROR,
                    "No execution provider registered for type: " + type
            );
        }
        TaskExecutionProvider provider = providers.get(type);
        if (provider == null) {
            throw new com.thesystem.common.exception.BusinessException(
                    com.thesystem.common.constants.ErrorCodes.VALIDATION_ERROR,
                    "No execution provider registered for type: " + type
            );
        }
        return provider;
    }

    public TaskExecutionState getProgress(Task task, TaskExecutionState state) {
        TaskExecutionProvider provider = getProvider(task.getExecutionType());
        return provider.calculateProgress(task, state);
    }

    public boolean isComplete(Task task, TaskExecutionState state) {
        TaskExecutionProvider provider = getProvider(task.getExecutionType());
        return provider.isComplete(task, state);
    }
}
