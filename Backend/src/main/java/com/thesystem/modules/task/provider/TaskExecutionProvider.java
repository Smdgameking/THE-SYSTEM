package com.thesystem.modules.task.provider;

import com.thesystem.modules.task.entity.Task;
import com.thesystem.modules.task.enums.TaskExecutionType;
import com.fasterxml.jackson.databind.ObjectMapper;

public interface TaskExecutionProvider {

    TaskExecutionState initialize(Task task);

    TaskExecutionState calculateProgress(Task task, TaskExecutionState state);

    boolean isComplete(Task task, TaskExecutionState state);

    void validate(Task task, TaskExecutionState state);

    TaskExecutionType getType();
}
