package com.thesystem.modules.task.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesystem.modules.task.dto.CreateTaskRequest;
import com.thesystem.modules.task.dto.DependencyResponse;
import com.thesystem.modules.task.dto.RecurringConfigResponse;
import com.thesystem.modules.task.dto.TaskResponse;
import com.thesystem.modules.task.dto.TimeEntryResponse;
import com.thesystem.modules.task.dto.UpdateTaskRequest;
import com.thesystem.modules.task.entity.RecurringTaskConfig;
import com.thesystem.modules.task.entity.Task;
import com.thesystem.modules.task.entity.TaskDependency;
import com.thesystem.modules.task.entity.TaskTimeEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "tags", source = "tags", qualifiedByName = "stringToList")
    @Mapping(target = "attachments", source = "attachments", qualifiedByName = "stringToMapList")
    @Mapping(target = "completionEvidence", source = "completionEvidence", qualifiedByName = "stringToMap")
    @Mapping(target = "executionState", source = "executionState", qualifiedByName = "stringToMap")
    @Mapping(target = "customMetadata", source = "customMetadata", qualifiedByName = "stringToMap")
    TaskResponse toTaskResponse(Task task);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "taskId", source = "taskId")
    @Mapping(target = "dependsOnTaskId", source = "dependsOnTaskId")
    @Mapping(target = "dependencyType", source = "dependencyType")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "resolvedDate", source = "resolvedDate")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "deletedAt", source = "deletedAt")
    DependencyResponse toDependencyResponse(TaskDependency dependency);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "taskId", source = "taskId")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "startTime", source = "startTime")
    @Mapping(target = "endTime", source = "endTime")
    @Mapping(target = "durationMinutes", source = "durationMinutes")
    @Mapping(target = "entryType", source = "entryType")
    @Mapping(target = "notes", source = "notes")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "createdBy", source = "createdBy")
    @Mapping(target = "deletedAt", source = "deletedAt")
    TimeEntryResponse toTimeEntryResponse(TaskTimeEntry timeEntry);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "taskId", source = "taskId")
    @Mapping(target = "frequency", source = "frequency")
    @Mapping(target = "intervalValue", source = "intervalValue")
    @Mapping(target = "cronExpression", source = "cronExpression")
    @Mapping(target = "daysOfWeek", source = "daysOfWeek", qualifiedByName = "stringToIntegerList")
    @Mapping(target = "dayOfMonth", source = "dayOfMonth")
    @Mapping(target = "month", source = "month")
    @Mapping(target = "exceptionDates", source = "exceptionDates", qualifiedByName = "stringToInstantList")
    @Mapping(target = "endDate", source = "endDate")
    @Mapping(target = "maxOccurrences", source = "maxOccurrences")
    @Mapping(target = "occurrenceCount", source = "occurrenceCount")
    @Mapping(target = "isActive", source = "isActive")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "updatedAt", source = "updatedAt")
    @Mapping(target = "createdBy", source = "createdBy")
    RecurringConfigResponse toRecurringConfigResponse(RecurringTaskConfig config);

    @Named("stringToList")
    default List<String> stringToList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Named("stringToMapList")
    default List<Map<String, Object>> stringToMapList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(value, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Named("stringToMap")
    default Map<String, Object> stringToMap(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(value, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    @Named("stringToIntegerList")
    default List<Integer> stringToIntegerList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(value, new TypeReference<List<Integer>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    @Named("stringToInstantList")
    default List<java.time.Instant> stringToInstantList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(value, new TypeReference<List<java.time.Instant>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    default String listToString(List<String> value) {
        if (value == null || value.isEmpty()) {
            return "[]";
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    default String mapListToString(List<Map<String, Object>> value) {
        if (value == null || value.isEmpty()) {
            return "[]";
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    default String mapToString(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return "{}";
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    default String integerListToString(List<Integer> value) {
        if (value == null || value.isEmpty()) {
            return "[]";
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    default String instantListToString(List<java.time.Instant> value) {
        if (value == null || value.isEmpty()) {
            return "[]";
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }
}
