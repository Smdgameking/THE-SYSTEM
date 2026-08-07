package com.thesystem.modules.task.controller;

import com.thesystem.common.response.ApiResponse;
import com.thesystem.modules.task.dto.CreateTaskRequest;
import com.thesystem.modules.task.dto.DependencyResponse;
import com.thesystem.modules.task.dto.RecurringConfigResponse;
import com.thesystem.modules.task.dto.TaskFilterRequest;
import com.thesystem.modules.task.dto.TaskResponse;
import com.thesystem.modules.task.dto.TaskStatisticsResponse;
import com.thesystem.modules.task.dto.TimeEntryResponse;
import com.thesystem.modules.task.dto.UpdateTaskRequest;
import com.thesystem.modules.task.enums.DependencyType;
import com.thesystem.modules.task.enums.TaskTimeEntryType;
import com.thesystem.modules.task.service.TaskService;
import com.thesystem.security.util.SecurityUtils;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @Operation(summary = "Create task")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(@Valid @RequestBody CreateTaskRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        TaskResponse response = taskService.createTask(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Task created successfully", UUID.randomUUID().toString()));
    }

    @GetMapping
    @Operation(summary = "List tasks")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasks(@Valid TaskFilterRequest filter) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<TaskResponse> response = taskService.listTasks(userId, filter);
        return ResponseEntity.ok(ApiResponse.ok(response, "Tasks retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(@PathVariable @Parameter(description = "Task ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        TaskResponse response = taskService.getTask(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Task retrieved successfully", UUID.randomUUID().toString()));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update task")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable @Parameter(description = "Task ID") UUID id,
            @Valid @RequestBody UpdateTaskRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        TaskResponse response = taskService.updateTask(userId, id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Task updated successfully", UUID.randomUUID().toString()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete task")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable @Parameter(description = "Task ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        taskService.deleteTask(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Task deleted successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete task")
    public ResponseEntity<ApiResponse<TaskResponse>> completeTask(@PathVariable @Parameter(description = "Task ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        TaskResponse response = taskService.completeTask(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Task completed successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/fail")
    @Operation(summary = "Fail task")
    public ResponseEntity<ApiResponse<TaskResponse>> failTask(
            @PathVariable @Parameter(description = "Task ID") UUID id,
            @RequestBody(required = false) String reason) {
        UUID userId = SecurityUtils.getCurrentUserId();
        TaskResponse response = taskService.failTask(userId, id, reason);
        return ResponseEntity.ok(ApiResponse.ok(response, "Task marked as failed", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel task")
    public ResponseEntity<ApiResponse<TaskResponse>> cancelTask(
            @PathVariable @Parameter(description = "Task ID") UUID id,
            @RequestBody(required = false) String reason) {
        UUID userId = SecurityUtils.getCurrentUserId();
        TaskResponse response = taskService.cancelTask(userId, id, reason);
        return ResponseEntity.ok(ApiResponse.ok(response, "Task cancelled successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive task")
    public ResponseEntity<ApiResponse<TaskResponse>> archiveTask(@PathVariable @Parameter(description = "Task ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        TaskResponse response = taskService.archiveTask(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Task archived successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore task")
    public ResponseEntity<ApiResponse<TaskResponse>> restoreTask(@PathVariable @Parameter(description = "Task ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        TaskResponse response = taskService.restoreTask(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Task restored successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/subtasks")
    @Operation(summary = "Create subtask")
    public ResponseEntity<ApiResponse<TaskResponse>> createSubtask(
            @PathVariable @Parameter(description = "Parent task ID") UUID id,
            @Valid @RequestBody CreateTaskRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        TaskResponse response = taskService.createSubtask(userId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Subtask created successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/{id}/subtasks")
    @Operation(summary = "List subtasks")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getSubtasks(@PathVariable @Parameter(description = "Parent task ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<TaskResponse> response = taskService.listSubtasks(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Subtasks retrieved successfully", UUID.randomUUID().toString()));
    }

    @DeleteMapping("/{id}/subtasks/{subtaskId}")
    @Operation(summary = "Delete subtask")
    public ResponseEntity<ApiResponse<Void>> deleteSubtask(
            @PathVariable @Parameter(description = "Parent task ID") UUID id,
            @PathVariable @Parameter(description = "Subtask ID") UUID subtaskId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        taskService.deleteSubtask(userId, id, subtaskId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Subtask deleted successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/dependencies")
    @Operation(summary = "Add dependency")
    public ResponseEntity<ApiResponse<DependencyResponse>> addDependency(
            @PathVariable @Parameter(description = "Task ID") UUID id,
            @Valid @RequestBody TaskService.AddDependencyRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        DependencyResponse response = taskService.addDependency(userId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Dependency added successfully", UUID.randomUUID().toString()));
    }

    @DeleteMapping("/{id}/dependencies/{dependsOnId}")
    @Operation(summary = "Remove dependency")
    public ResponseEntity<ApiResponse<Void>> removeDependency(
            @PathVariable @Parameter(description = "Task ID") UUID id,
            @PathVariable @Parameter(description = "Dependency task ID") UUID dependsOnId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        taskService.removeDependency(userId, id, dependsOnId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Dependency removed successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/{id}/dependencies")
    @Operation(summary = "List dependencies")
    public ResponseEntity<ApiResponse<List<DependencyResponse>>> getDependencies(@PathVariable @Parameter(description = "Task ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<DependencyResponse> response = taskService.listDependencies(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Dependencies retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/{id}/dependents")
    @Operation(summary = "List dependents")
    public ResponseEntity<ApiResponse<List<DependencyResponse>>> getDependents(@PathVariable @Parameter(description = "Task ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<DependencyResponse> response = taskService.listDependents(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Dependents retrieved successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/time-entries")
    @Operation(summary = "Start time entry")
    public ResponseEntity<ApiResponse<TimeEntryResponse>> startTimeEntry(
            @PathVariable @Parameter(description = "Task ID") UUID id,
            @Valid @RequestBody TaskService.StartTimeEntryRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        TimeEntryResponse response = taskService.startTimeEntry(userId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Time entry started successfully", UUID.randomUUID().toString()));
    }

    @PatchMapping("/{id}/time-entries/{entryId}")
    @Operation(summary = "Stop time entry")
    public ResponseEntity<ApiResponse<TimeEntryResponse>> stopTimeEntry(
            @PathVariable @Parameter(description = "Task ID") UUID id,
            @PathVariable @Parameter(description = "Time entry ID") UUID entryId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        TimeEntryResponse response = taskService.stopTimeEntry(userId, id, entryId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Time entry stopped successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/{id}/time-entries")
    @Operation(summary = "List time entries")
    public ResponseEntity<ApiResponse<List<TimeEntryResponse>>> getTimeEntries(@PathVariable @Parameter(description = "Task ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<TimeEntryResponse> response = taskService.listTimeEntries(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Time entries retrieved successfully", UUID.randomUUID().toString()));
    }

    @DeleteMapping("/{id}/time-entries/{entryId}")
    @Operation(summary = "Delete time entry")
    public ResponseEntity<ApiResponse<Void>> deleteTimeEntry(
            @PathVariable @Parameter(description = "Task ID") UUID id,
            @PathVariable @Parameter(description = "Time entry ID") UUID entryId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        taskService.deleteTimeEntry(userId, id, entryId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Time entry deleted successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get task statistics")
    public ResponseEntity<ApiResponse<TaskStatisticsResponse>> getStatistics() {
        UUID userId = SecurityUtils.getCurrentUserId();
        TaskStatisticsResponse response = taskService.getStatistics(userId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Statistics retrieved successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/recurrence")
    @Operation(summary = "Configure recurrence")
    public ResponseEntity<ApiResponse<RecurringConfigResponse>> configureRecurrence(
            @PathVariable @Parameter(description = "Task ID") UUID id,
            @Valid @RequestBody TaskService.UpdateRecurrenceRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        RecurringConfigResponse response = taskService.configureRecurrence(userId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Recurrence configured successfully", UUID.randomUUID().toString()));
    }

    @PatchMapping("/{id}/recurrence")
    @Operation(summary = "Update recurrence")
    public ResponseEntity<ApiResponse<RecurringConfigResponse>> updateRecurrence(
            @PathVariable @Parameter(description = "Task ID") UUID id,
            @Valid @RequestBody TaskService.UpdateRecurrenceRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        RecurringConfigResponse response = taskService.updateRecurrence(userId, id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Recurrence updated successfully", UUID.randomUUID().toString()));
    }

    @DeleteMapping("/{id}/recurrence")
    @Operation(summary = "Remove recurrence")
    public ResponseEntity<ApiResponse<Void>> removeRecurrence(@PathVariable @Parameter(description = "Task ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        taskService.removeRecurrence(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Recurrence removed successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/attachments")
    @Operation(summary = "Add attachment")
    public ResponseEntity<ApiResponse<TaskResponse>> addAttachment(
            @PathVariable @Parameter(description = "Task ID") UUID id,
            @Valid @RequestBody TaskService.AddAttachmentRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        TaskResponse response = taskService.addAttachment(userId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Attachment added successfully", UUID.randomUUID().toString()));
    }

    @DeleteMapping("/{id}/attachments/{attachmentId}")
    @Operation(summary = "Remove attachment")
    public ResponseEntity<ApiResponse<Void>> removeAttachment(
            @PathVariable @Parameter(description = "Task ID") UUID id,
            @PathVariable @Parameter(description = "Attachment ID") UUID attachmentId) {
        UUID userId = SecurityUtils.getCurrentUserId();
        taskService.removeAttachment(userId, id, attachmentId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Attachment removed successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/{id}/attachments")
    @Operation(summary = "List attachments")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAttachments(@PathVariable @Parameter(description = "Task ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<Map<String, Object>> response = taskService.listAttachments(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Attachments retrieved successfully", UUID.randomUUID().toString()));
    }
}
