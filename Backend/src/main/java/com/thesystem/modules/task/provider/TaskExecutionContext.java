package com.thesystem.modules.task.provider;

import com.thesystem.modules.task.entity.Task;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class TaskExecutionContext {

    private final Task task;
    private final ObjectMapper objectMapper;
    private final Map<String, Object> context;

    public TaskExecutionContext(Task task, ObjectMapper objectMapper, Map<String, Object> context) {
        this.task = task;
        this.objectMapper = objectMapper;
        this.context = context;
    }

    public Task getTask() {
        return task;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public Map<String, Object> getContext() {
        return context;
    }
}
