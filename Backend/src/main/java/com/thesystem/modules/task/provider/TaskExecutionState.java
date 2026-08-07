package com.thesystem.modules.task.provider;

import com.thesystem.modules.task.entity.Task;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TaskExecutionState {

    private String rawJson;
    private java.util.Map<String, Object> data;
    private ObjectMapper objectMapper;

    public TaskExecutionState(String rawJson, ObjectMapper objectMapper) {
        this.rawJson = rawJson;
        this.objectMapper = objectMapper;
        this.data = parseJson(rawJson);
    }

    public String toJson() {
        try {
            if (data != null) {
                this.rawJson = objectMapper.writeValueAsString(data);
            }
            return rawJson;
        } catch (Exception e) {
            throw new com.thesystem.common.exception.BusinessException(
                    com.thesystem.common.constants.ErrorCodes.INTERNAL_ERROR,
                    "Failed to serialize execution state: " + e.getMessage()
            );
        }
    }

    public TaskExecutionState fromJson(String json) {
        this.rawJson = json;
        this.data = parseJson(json);
        return this;
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) {
            return new java.util.HashMap<>();
        }
        try {
            return objectMapper.readValue(json, java.util.Map.class);
        } catch (Exception e) {
            throw new com.thesystem.common.exception.BusinessException(
                    com.thesystem.common.constants.ErrorCodes.VALIDATION_ERROR,
                    "Failed to parse execution state JSON: " + e.getMessage()
            );
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        if (data == null || !data.containsKey(key)) {
            return null;
        }
        return (T) data.get(key);
    }

    public void put(String key, Object value) {
        if (data == null) {
            data = new java.util.HashMap<>();
        }
        data.put(key, value);
    }

    public String getRawJson() {
        return rawJson;
    }

    public java.util.Map<String, Object> getData() {
        return data;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
