package com.thesystem.modules.task.service;

import com.thesystem.modules.task.dto.CreateTaskRequest;
import com.thesystem.modules.task.dto.DependencyResponse;
import com.thesystem.modules.task.dto.RecurringConfigResponse;
import com.thesystem.modules.task.dto.TaskFilterRequest;
import com.thesystem.modules.task.dto.TaskResponse;
import com.thesystem.modules.task.dto.TaskStatisticsResponse;
import com.thesystem.modules.task.dto.TimeEntryResponse;
import com.thesystem.modules.task.dto.UpdateTaskRequest;

import com.thesystem.modules.task.enums.DependencyType;
import com.thesystem.modules.task.enums.RecurrenceFrequency;
import com.thesystem.modules.task.enums.TaskTimeEntryType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface TaskService {

    /**
     * Creates a new task for the specified user.
     *
     * @param userId  the ID of the task owner
     * @param request the task creation request containing task details
     * @return the created task response
     */
    TaskResponse createTask(UUID userId, CreateTaskRequest request);

    /**
     * Retrieves a task by its ID.
     *
     * @param userId the ID of the task owner
     * @param taskId the ID of the task to retrieve
     * @return the task response
     */
    TaskResponse getTask(UUID userId, UUID taskId);

    /**
     * Lists tasks for a user with optional filtering and pagination.
     *
     * @param userId the ID of the task owner
     * @param filter the filter criteria for tasks
     * @return a list of task responses
     */
    List<TaskResponse> listTasks(UUID userId, TaskFilterRequest filter);

    /**
     * Updates an existing task.
     *
     * @param userId  the ID of the task owner
     * @param taskId  the ID of the task to update
     * @param request the update request containing modified fields
     * @return the updated task response
     */
    TaskResponse updateTask(UUID userId, UUID taskId, UpdateTaskRequest request);

    /**
     * Soft-deletes a task.
     *
     * @param userId the ID of the task owner
     * @param taskId the ID of the task to delete
     */
    void deleteTask(UUID userId, UUID taskId);

    /**
     * Marks a task as completed.
     *
     * @param userId the ID of the task owner
     * @param taskId the ID of the task to complete
     * @return the completed task response
     */
    TaskResponse completeTask(UUID userId, UUID taskId);

    /**
     * Marks a task as failed with a reason.
     *
     * @param userId the ID of the task owner
     * @param taskId the ID of the task to fail
     * @param reason the failure reason
     * @return the failed task response
     */
    TaskResponse failTask(UUID userId, UUID taskId, String reason);

    /**
     * Cancels a task with a reason.
     *
     * @param userId the ID of the task owner
     * @param taskId the ID of the task to cancel
     * @param reason the cancellation reason
     * @return the cancelled task response
     */
    TaskResponse cancelTask(UUID userId, UUID taskId, String reason);

    /**
     * Archives a completed or cancelled task.
     *
     * @param userId the ID of the task owner
     * @param taskId the ID of the task to archive
     * @return the archived task response
     */
    TaskResponse archiveTask(UUID userId, UUID taskId);

    /**
     * Restores a deleted or archived task.
     *
     * @param userId the ID of the task owner
     * @param taskId the ID of the task to restore
     * @return the restored task response
     */
    TaskResponse restoreTask(UUID userId, UUID taskId);

    /**
     * Creates a subtask under a parent task.
     *
     * @param userId      the ID of the task owner
     * @param parentTaskId the ID of the parent task
     * @param request     the subtask creation request
     * @return the created subtask response
     */
    TaskResponse createSubtask(UUID userId, UUID parentTaskId, CreateTaskRequest request);

    /**
     * Lists all subtasks of a parent task.
     *
     * @param userId      the ID of the task owner
     * @param parentTaskId the ID of the parent task
     * @return a list of subtask responses
     */
    List<TaskResponse> listSubtasks(UUID userId, UUID parentTaskId);

    /**
     * Deletes a subtask from its parent task.
     *
     * @param userId      the ID of the task owner
     * @param parentTaskId the ID of the parent task
     * @param subtaskId   the ID of the subtask to delete
     */
    void deleteSubtask(UUID userId, UUID parentTaskId, UUID subtaskId);

    /**
     * Adds a dependency to a task.
     *
     * @param userId   the ID of the task owner
     * @param taskId   the ID of the task
     * @param request  the dependency creation request
     * @return the created dependency response
     */
    DependencyResponse addDependency(UUID userId, UUID taskId, AddDependencyRequest request);

    /**
     * Removes a dependency from a task.
     *
     * @param userId          the ID of the task owner
     * @param taskId          the ID of the task
     * @param dependsOnTaskId the ID of the dependency task to remove
     */
    void removeDependency(UUID userId, UUID taskId, UUID dependsOnTaskId);

    /**
     * Lists all dependencies of a task.
     *
     * @param userId the ID of the task owner
     * @param taskId the ID of the task
     * @return a list of dependency responses
     */
    List<DependencyResponse> listDependencies(UUID userId, UUID taskId);

    /**
     * Lists all tasks that depend on the specified task.
     *
     * @param userId the ID of the task owner
     * @param taskId the ID of the task
     * @return a list of dependency responses for dependents
     */
    List<DependencyResponse> listDependents(UUID userId, UUID taskId);

    /**
     * Starts a time entry for a task.
     *
     * @param userId  the ID of the task owner
     * @param taskId  the ID of the task
     * @param request the time entry start request
     * @return the started time entry response
     */
    TimeEntryResponse startTimeEntry(UUID userId, UUID taskId, StartTimeEntryRequest request);

    /**
     * Stops an active time entry for a task.
     *
     * @param userId  the ID of the task owner
     * @param taskId  the ID of the task
     * @param entryId the ID of the time entry to stop
     * @return the stopped time entry response
     */
    TimeEntryResponse stopTimeEntry(UUID userId, UUID taskId, UUID entryId);

    /**
     * Lists all time entries for a task.
     *
     * @param userId the ID of the task owner
     * @param taskId the ID of the task
     * @return a list of time entry responses
     */
    List<TimeEntryResponse> listTimeEntries(UUID userId, UUID taskId);

    /**
     * Deletes a time entry from a task.
     *
     * @param userId  the ID of the task owner
     * @param taskId  the ID of the task
     * @param entryId the ID of the time entry to delete
     */
    void deleteTimeEntry(UUID userId, UUID taskId, UUID entryId);

    /**
     * Retrieves task statistics for a user.
     *
     * @param userId the ID of the task owner
     * @return the task statistics response
     */
    TaskStatisticsResponse getStatistics(UUID userId);

    /**
     * Configures recurrence for a task.
     *
     * @param userId  the ID of the task owner
     * @param taskId  the ID of the task
     * @param request the recurrence configuration request
     * @return the recurring config response
     */
    RecurringConfigResponse configureRecurrence(UUID userId, UUID taskId, UpdateRecurrenceRequest request);

    /**
     * Updates the recurrence configuration for a task.
     *
     * @param userId  the ID of the task owner
     * @param taskId  the ID of the task
     * @param request the updated recurrence configuration request
     * @return the updated recurring config response
     */
    RecurringConfigResponse updateRecurrence(UUID userId, UUID taskId, UpdateRecurrenceRequest request);

    /**
     * Removes recurrence configuration from a task.
     *
     * @param userId the ID of the task owner
     * @param taskId the ID of the task
     */
    void removeRecurrence(UUID userId, UUID taskId);

    /**
     * Adds an attachment to a task.
     *
     * @param userId  the ID of the task owner
     * @param taskId  the ID of the task
     * @param request the attachment creation request
     * @return the updated task response with attachment metadata
     */
    TaskResponse addAttachment(UUID userId, UUID taskId, AddAttachmentRequest request);

    /**
     * Removes an attachment from a task.
     *
     * @param userId        the ID of the task owner
     * @param taskId        the ID of the task
     * @param attachmentId the ID of the attachment to remove
     */
    void removeAttachment(UUID userId, UUID taskId, UUID attachmentId);

    /**
     * Lists all attachments for a task.
     *
     * @param userId the ID of the task owner
     * @param taskId the ID of the task
     * @return a list of attachment metadata maps
     */
    List<Map<String, Object>> listAttachments(UUID userId, UUID taskId);

    record AddDependencyRequest(
            UUID dependsOnTaskId,
            DependencyType dependencyType
    ) {
    }

    record StartTimeEntryRequest(
            TaskTimeEntryType entryType,
            String notes
    ) {
    }

    record UpdateRecurrenceRequest(
            RecurrenceFrequency frequency,
            Integer intervalValue,
            String cronExpression,
            List<Integer> daysOfWeek,
            Integer dayOfMonth,
            Integer month,
            List<Instant> exceptionDates,
            Instant endDate,
            Integer maxOccurrences,
            Boolean isActive
    ) {
    }

    record AddAttachmentRequest(
            AttachmentType type,
            String name,
            String url,
            UUID fileId,
            Long sizeBytes,
            String mimeType
    ) {
    }

    enum AttachmentType {
        FILE,
        LINK,
        IMAGE
    }
}
