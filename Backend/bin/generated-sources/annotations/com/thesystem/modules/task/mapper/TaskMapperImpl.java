package com.thesystem.modules.task.mapper;

import com.thesystem.modules.task.dto.DependencyResponse;
import com.thesystem.modules.task.dto.RecurringConfigResponse;
import com.thesystem.modules.task.dto.TaskResponse;
import com.thesystem.modules.task.dto.TimeEntryResponse;
import com.thesystem.modules.task.entity.RecurringTaskConfig;
import com.thesystem.modules.task.entity.Task;
import com.thesystem.modules.task.entity.TaskDependency;
import com.thesystem.modules.task.entity.TaskTimeEntry;
import com.thesystem.modules.task.enums.DependencyStatus;
import com.thesystem.modules.task.enums.DependencyType;
import com.thesystem.modules.task.enums.RecurrenceFrequency;
import com.thesystem.modules.task.enums.TaskExecutionType;
import com.thesystem.modules.task.enums.TaskPriority;
import com.thesystem.modules.task.enums.TaskStatus;
import com.thesystem.modules.task.enums.TaskTimeEntryType;
import com.thesystem.modules.task.enums.TaskVisibility;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-09T03:54:43+0530",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.100.v20260624-0231, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class TaskMapperImpl implements TaskMapper {

    @Override
    public TaskResponse toTaskResponse(Task task) {
        if ( task == null ) {
            return null;
        }

        List<String> tags = null;
        List<Map<String, Object>> attachments = null;
        Map<String, Object> completionEvidence = null;
        Map<String, Object> executionState = null;
        Map<String, Object> customMetadata = null;
        UUID id = null;
        UUID userId = null;
        UUID goalId = null;
        UUID parentTaskId = null;
        String title = null;
        String description = null;
        TaskStatus status = null;
        TaskPriority priority = null;
        String category = null;
        TaskExecutionType executionType = null;
        Integer estimatedDuration = null;
        Integer actualDuration = null;
        Instant startDate = null;
        Instant dueDate = null;
        Instant completedDate = null;
        Instant reminderDate = null;
        Boolean isRecurring = null;
        UUID recurringConfigId = null;
        String notes = null;
        TaskVisibility visibility = null;
        Instant createdAt = null;
        Instant updatedAt = null;
        UUID createdBy = null;
        UUID updatedBy = null;
        Instant deletedAt = null;

        tags = stringToList( task.getTags() );
        attachments = stringToMapList( task.getAttachments() );
        completionEvidence = stringToMap( task.getCompletionEvidence() );
        executionState = stringToMap( task.getExecutionState() );
        customMetadata = stringToMap( task.getCustomMetadata() );
        id = task.getId();
        userId = task.getUserId();
        goalId = task.getGoalId();
        parentTaskId = task.getParentTaskId();
        title = task.getTitle();
        description = task.getDescription();
        status = task.getStatus();
        priority = task.getPriority();
        category = task.getCategory();
        executionType = task.getExecutionType();
        estimatedDuration = task.getEstimatedDuration();
        actualDuration = task.getActualDuration();
        startDate = task.getStartDate();
        dueDate = task.getDueDate();
        completedDate = task.getCompletedDate();
        reminderDate = task.getReminderDate();
        isRecurring = task.getIsRecurring();
        recurringConfigId = task.getRecurringConfigId();
        notes = task.getNotes();
        visibility = task.getVisibility();
        createdAt = task.getCreatedAt();
        updatedAt = task.getUpdatedAt();
        createdBy = task.getCreatedBy();
        updatedBy = task.getUpdatedBy();
        deletedAt = task.getDeletedAt();

        TaskResponse taskResponse = new TaskResponse( id, userId, goalId, parentTaskId, title, description, status, priority, category, executionType, estimatedDuration, actualDuration, startDate, dueDate, completedDate, reminderDate, isRecurring, recurringConfigId, tags, attachments, notes, completionEvidence, executionState, customMetadata, visibility, createdAt, updatedAt, createdBy, updatedBy, deletedAt );

        return taskResponse;
    }

    @Override
    public DependencyResponse toDependencyResponse(TaskDependency dependency) {
        if ( dependency == null ) {
            return null;
        }

        UUID id = null;
        UUID taskId = null;
        UUID dependsOnTaskId = null;
        DependencyType dependencyType = null;
        DependencyStatus status = null;
        Instant resolvedDate = null;
        Instant createdAt = null;
        UUID createdBy = null;
        Instant deletedAt = null;

        id = dependency.getId();
        taskId = dependency.getTaskId();
        dependsOnTaskId = dependency.getDependsOnTaskId();
        dependencyType = dependency.getDependencyType();
        status = dependency.getStatus();
        resolvedDate = dependency.getResolvedDate();
        createdAt = dependency.getCreatedAt();
        createdBy = dependency.getCreatedBy();
        deletedAt = dependency.getDeletedAt();

        DependencyResponse dependencyResponse = new DependencyResponse( id, taskId, dependsOnTaskId, dependencyType, status, resolvedDate, createdAt, createdBy, deletedAt );

        return dependencyResponse;
    }

    @Override
    public TimeEntryResponse toTimeEntryResponse(TaskTimeEntry timeEntry) {
        if ( timeEntry == null ) {
            return null;
        }

        UUID id = null;
        UUID taskId = null;
        UUID userId = null;
        Instant startTime = null;
        Instant endTime = null;
        Integer durationMinutes = null;
        TaskTimeEntryType entryType = null;
        String notes = null;
        Instant createdAt = null;
        UUID createdBy = null;
        Instant deletedAt = null;

        id = timeEntry.getId();
        taskId = timeEntry.getTaskId();
        userId = timeEntry.getUserId();
        startTime = timeEntry.getStartTime();
        endTime = timeEntry.getEndTime();
        durationMinutes = timeEntry.getDurationMinutes();
        entryType = timeEntry.getEntryType();
        notes = timeEntry.getNotes();
        createdAt = timeEntry.getCreatedAt();
        createdBy = timeEntry.getCreatedBy();
        deletedAt = timeEntry.getDeletedAt();

        TimeEntryResponse timeEntryResponse = new TimeEntryResponse( id, taskId, userId, startTime, endTime, durationMinutes, entryType, notes, createdAt, createdBy, deletedAt );

        return timeEntryResponse;
    }

    @Override
    public RecurringConfigResponse toRecurringConfigResponse(RecurringTaskConfig config) {
        if ( config == null ) {
            return null;
        }

        UUID id = null;
        UUID taskId = null;
        RecurrenceFrequency frequency = null;
        Integer intervalValue = null;
        String cronExpression = null;
        List<Integer> daysOfWeek = null;
        Integer dayOfMonth = null;
        Integer month = null;
        List<Instant> exceptionDates = null;
        Instant endDate = null;
        Integer maxOccurrences = null;
        Integer occurrenceCount = null;
        Boolean isActive = null;
        Instant createdAt = null;
        Instant updatedAt = null;
        UUID createdBy = null;

        id = config.getId();
        taskId = config.getTaskId();
        frequency = config.getFrequency();
        intervalValue = config.getIntervalValue();
        cronExpression = config.getCronExpression();
        daysOfWeek = stringToIntegerList( config.getDaysOfWeek() );
        dayOfMonth = config.getDayOfMonth();
        month = config.getMonth();
        exceptionDates = stringToInstantList( config.getExceptionDates() );
        endDate = config.getEndDate();
        maxOccurrences = config.getMaxOccurrences();
        occurrenceCount = config.getOccurrenceCount();
        isActive = config.getIsActive();
        createdAt = config.getCreatedAt();
        updatedAt = config.getUpdatedAt();
        createdBy = config.getCreatedBy();

        RecurringConfigResponse recurringConfigResponse = new RecurringConfigResponse( id, taskId, frequency, intervalValue, cronExpression, daysOfWeek, dayOfMonth, month, exceptionDates, endDate, maxOccurrences, occurrenceCount, isActive, createdAt, updatedAt, createdBy );

        return recurringConfigResponse;
    }
}
