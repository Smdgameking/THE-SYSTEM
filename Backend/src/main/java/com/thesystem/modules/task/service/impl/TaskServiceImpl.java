package com.thesystem.modules.task.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesystem.common.exception.BusinessException;
import com.thesystem.common.constants.ErrorCodes;
import com.thesystem.modules.task.dto.CreateTaskRequest;
import com.thesystem.modules.task.dto.DependencyResponse;
import com.thesystem.modules.task.dto.RecurringConfigResponse;
import com.thesystem.modules.task.dto.TaskFilterRequest;
import com.thesystem.modules.task.dto.TaskResponse;
import com.thesystem.modules.task.dto.TaskStatisticsResponse;
import com.thesystem.modules.task.dto.TimeEntryResponse;
import com.thesystem.modules.task.dto.UpdateTaskRequest;
import com.thesystem.modules.task.entity.RecurringTaskConfig;
import com.thesystem.modules.task.entity.Task;
import com.thesystem.modules.task.entity.TaskDependency;
import com.thesystem.modules.task.entity.TaskTimeEntry;
import com.thesystem.modules.task.enums.DependencyStatus;
import com.thesystem.modules.task.enums.DependencyType;
import com.thesystem.modules.task.enums.TaskDifficulty;
import com.thesystem.modules.task.enums.TaskExecutionType;
import com.thesystem.modules.task.enums.TaskPriority;
import com.thesystem.modules.task.enums.TaskStatus;
import com.thesystem.modules.task.enums.TaskTimeEntryType;
import com.thesystem.modules.task.enums.TaskVisibility;
import com.thesystem.modules.task.events.*;
import com.thesystem.modules.task.mapper.TaskMapper;
import com.thesystem.modules.task.provider.TaskExecutionProviderRegistry;
import com.thesystem.modules.task.provider.TaskExecutionState;
import com.thesystem.modules.task.repository.RecurringTaskConfigRepository;
import com.thesystem.modules.task.repository.TaskDependencyRepository;
import com.thesystem.modules.task.repository.TaskRepository;
import com.thesystem.modules.task.repository.TaskTimeEntryRepository;
import com.thesystem.modules.task.service.TaskService;
import com.thesystem.modules.user.service.UserTimezoneResolver;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TaskDependencyRepository taskDependencyRepository;
    private final TaskTimeEntryRepository taskTimeEntryRepository;
    private final RecurringTaskConfigRepository recurringTaskConfigRepository;
    private final TaskMapper taskMapper;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final TaskExecutionProviderRegistry executionProviderRegistry;
    private final UserTimezoneResolver userTimezoneResolver;

    public TaskServiceImpl(
            TaskRepository taskRepository,
            TaskDependencyRepository taskDependencyRepository,
            TaskTimeEntryRepository taskTimeEntryRepository,
            RecurringTaskConfigRepository recurringTaskConfigRepository,
            TaskMapper taskMapper,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            TaskExecutionProviderRegistry executionProviderRegistry,
            UserTimezoneResolver userTimezoneResolver
    ) {
        this.taskRepository = taskRepository;
        this.taskDependencyRepository = taskDependencyRepository;
        this.taskTimeEntryRepository = taskTimeEntryRepository;
        this.recurringTaskConfigRepository = recurringTaskConfigRepository;
        this.taskMapper = taskMapper;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.executionProviderRegistry = executionProviderRegistry;
        this.userTimezoneResolver = userTimezoneResolver;
    }

    @Override
    @Transactional
    public TaskResponse createTask(UUID userId, CreateTaskRequest request) {
        Task task = new Task();
        task.setUserId(userId);
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setGoalId(request.goalId());
        task.setParentTaskId(request.parentTaskId());
        task.setStatus(request.status() != null ? request.status() : TaskStatus.DRAFT);
        task.setPriority(request.priority() != null ? request.priority() : TaskPriority.NORMAL);
        task.setDifficulty(request.difficulty() != null ? request.difficulty() : TaskDifficulty.NORMAL);
        task.setCategory(request.category());
        task.setExecutionType(request.executionType() != null ? request.executionType() : TaskExecutionType.BOOLEAN);
        task.setEstimatedDuration(request.estimatedDuration());
        task.setStartDate(request.startDate());
        task.setDueDate(request.dueDate());
        task.setReminderDate(request.reminderDate());
        task.setIsRecurring(false);
        task.setTags(toJson(request.tags()));
        task.setAttachments(toJson(request.attachments()));
        task.setNotes(request.notes());
        task.setCompletionEvidence(toJson(request.completionEvidence()));
        task.setCustomMetadata(toJson(request.customMetadata()));
        task.setVisibility(request.visibility() != null ? request.visibility() : TaskVisibility.PRIVATE);

        if (task.getExecutionType() != null) {
            TaskExecutionState initialState = executionProviderRegistry.getProvider(task.getExecutionType()).initialize(task);
            task.setExecutionState(initialState.toJson());
        }

        Task saved = taskRepository.save(task);
        eventPublisher.publishEvent(new TaskCreatedEvent(toLong(saved.getId()), toLong(userId), toLong(saved.getGoalId()), saved.getTitle()));
        return taskMapper.toTaskResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID userId, UUID taskId) {
        Task task = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Task not found"));
        return taskMapper.toTaskResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks(UUID userId, TaskFilterRequest filter) {
        List<Task> tasks;
        if (filter.status() != null && !filter.status().isEmpty()) {
            tasks = taskRepository.findByUserIdAndStatusAndDeletedAtIsNull(userId, filter.status().get(0));
        } else if (filter.priority() != null && !filter.priority().isEmpty()) {
            tasks = taskRepository.findByUserIdAndPriorityAndDeletedAtIsNull(userId, filter.priority().get(0));
        } else if (filter.goalId() != null) {
            tasks = taskRepository.findByUserIdAndGoalIdAndDeletedAtIsNull(userId, filter.goalId());
        } else if (filter.parentTaskId() != null) {
            tasks = taskRepository.findByUserIdAndParentTaskIdAndDeletedAtIsNull(userId, filter.parentTaskId());
        } else if (filter.executionType() != null) {
            tasks = taskRepository.findByUserIdAndExecutionTypeAndDeletedAtIsNull(userId, filter.executionType());
        } else {
            tasks = taskRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
        }
        return tasks.stream().map(taskMapper::toTaskResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TaskResponse updateTask(UUID userId, UUID taskId, UpdateTaskRequest request) {
        Task task = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Task not found"));

        if (request.title() != null) task.setTitle(request.title());
        if (request.description() != null) task.setDescription(request.description());
        if (request.goalId() != null) task.setGoalId(request.goalId());
        if (request.parentTaskId() != null) task.setParentTaskId(request.parentTaskId());
        if (request.priority() != null) task.setPriority(request.priority());
        if (request.status() != null) task.setStatus(request.status());
        if (request.difficulty() != null) task.setDifficulty(request.difficulty());
        if (request.category() != null) task.setCategory(request.category());
        if (request.executionType() != null) task.setExecutionType(request.executionType());
        if (request.estimatedDuration() != null) task.setEstimatedDuration(request.estimatedDuration());
        if (request.actualDuration() != null) task.setActualDuration(request.actualDuration());
        if (request.startDate() != null) task.setStartDate(request.startDate());
        if (request.dueDate() != null) task.setDueDate(request.dueDate());
        if (request.completedDate() != null) task.setCompletedDate(request.completedDate());
        if (request.reminderDate() != null) task.setReminderDate(request.reminderDate());
        if (request.isRecurring() != null) task.setIsRecurring(request.isRecurring());
        if (request.recurringConfigId() != null) task.setRecurringConfigId(request.recurringConfigId());
        if (request.tags() != null) task.setTags(toJson(request.tags()));
        if (request.attachments() != null) task.setAttachments(toJson(request.attachments()));
        if (request.notes() != null) task.setNotes(request.notes());
        if (request.completionEvidence() != null) task.setCompletionEvidence(toJson(request.completionEvidence()));
        if (request.executionState() != null) task.setExecutionState(toJson(request.executionState()));
        if (request.customMetadata() != null) task.setCustomMetadata(toJson(request.customMetadata()));
        if (request.visibility() != null) task.setVisibility(request.visibility());

        Task saved = taskRepository.save(task);
        eventPublisher.publishEvent(new TaskUpdatedEvent(toLong(saved.getId()), toLong(userId)));
        return taskMapper.toTaskResponse(saved);
    }

    @Override
    @Transactional
    public void deleteTask(UUID userId, UUID taskId) {
        Task task = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Task not found"));
        task.setDeletedAt(Instant.now());
        taskRepository.save(task);
        eventPublisher.publishEvent(new TaskDeletedEvent(toLong(taskId), toLong(userId)));
    }

    @Override
    @Transactional
    public TaskResponse completeTask(UUID userId, UUID taskId) {
        Task task = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Task not found"));
        validateTransition(task.getStatus(), TaskStatus.COMPLETED);
        validateSubtasksCompleted(taskId, userId);
        task.setStatus(TaskStatus.COMPLETED);
        Instant completedAt = Instant.now();
        task.setCompletedDate(completedAt);
        Task saved = taskRepository.save(task);
        eventPublisher.publishEvent(new TaskCompletedEvent(saved.getId(), userId, saved.getGoalId(), saved.getTitle(), saved.getExecutionType() != null ? saved.getExecutionType().name() : null, saved.getDifficulty() != null ? saved.getDifficulty().name() : null, completedAt));
        return taskMapper.toTaskResponse(saved);
    }

    @Override
    @Transactional
    public TaskResponse failTask(UUID userId, UUID taskId, String reason) {
        Task task = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Task not found"));
        validateTransition(task.getStatus(), TaskStatus.FAILED);
        task.setStatus(TaskStatus.FAILED);
        task.setCustomMetadata(reason != null ? toJson(Map.of("failureReason", reason)) : task.getCustomMetadata());
        Task saved = taskRepository.save(task);
        eventPublisher.publishEvent(new TaskFailedEvent(toLong(saved.getId()), toLong(userId), reason));
        return taskMapper.toTaskResponse(saved);
    }

    @Override
    @Transactional
    public TaskResponse cancelTask(UUID userId, UUID taskId, String reason) {
        Task task = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Task not found"));
        validateTransition(task.getStatus(), TaskStatus.CANCELLED);
        task.setStatus(TaskStatus.CANCELLED);
        task.setCustomMetadata(reason != null ? toJson(Map.of("cancelReason", reason)) : task.getCustomMetadata());
        Task saved = taskRepository.save(task);
        eventPublisher.publishEvent(new TaskCancelledEvent(toLong(saved.getId()), toLong(userId), reason));
        return taskMapper.toTaskResponse(saved);
    }

    @Override
    @Transactional
    public TaskResponse archiveTask(UUID userId, UUID taskId) {
        Task task = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Task not found"));
        validateTransition(task.getStatus(), TaskStatus.ARCHIVED);
        task.setStatus(TaskStatus.ARCHIVED);
        Task saved = taskRepository.save(task);
        eventPublisher.publishEvent(new TaskArchivedEvent(toLong(saved.getId()), toLong(userId)));
        return taskMapper.toTaskResponse(saved);
    }

    @Override
    @Transactional
    public TaskResponse restoreTask(UUID userId, UUID taskId) {
        Task task = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Task not found"));
        validateTransition(task.getStatus(), TaskStatus.DRAFT);
        task.setStatus(TaskStatus.DRAFT);
        task.setDeletedAt(null);
        Task saved = taskRepository.save(task);
        eventPublisher.publishEvent(new TaskActivatedEvent(toLong(saved.getId()), toLong(userId)));
        return taskMapper.toTaskResponse(saved);
    }

    @Override
    @Transactional
    public TaskResponse createSubtask(UUID userId, UUID parentTaskId, CreateTaskRequest request) {
        Task parent = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(parentTaskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Parent task not found"));
        CreateTaskRequest subtaskRequest = new CreateTaskRequest(
                request.title(), request.description(), request.goalId(), parentTaskId,
                request.status(), request.priority(), request.difficulty(), request.category(), request.executionType(),
                request.estimatedDuration(), request.startDate(), request.dueDate(), request.reminderDate(),
                request.tags(), request.attachments(), request.notes(), request.completionEvidence(),
                request.executionState(), request.customMetadata(), request.visibility()
        );
        TaskResponse response = createTask(userId, subtaskRequest);
        eventPublisher.publishEvent(new TaskSubtaskCreatedEvent(toLong(parentTaskId), toLong(response.id())));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> listSubtasks(UUID userId, UUID parentTaskId) {
        return taskRepository.findByUserIdAndParentTaskIdAndDeletedAtIsNull(userId, parentTaskId)
                .stream().map(taskMapper::toTaskResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSubtask(UUID userId, UUID parentTaskId, UUID subtaskId) {
        Task subtask = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(subtaskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Subtask not found"));
        if (!parentTaskId.equals(subtask.getParentTaskId())) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Subtask does not belong to parent task");
        }
        subtask.setDeletedAt(Instant.now());
        taskRepository.save(subtask);
    }

    @Override
    @Transactional
    public DependencyResponse addDependency(UUID userId, UUID taskId, TaskService.AddDependencyRequest request) {
        Task task = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Task not found"));
        Task dependsOn = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(request.dependsOnTaskId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Dependency task not found"));

        List<TaskDependency> existing = taskDependencyRepository.findByTaskIdAndDeletedAtIsNull(taskId);
        for (TaskDependency dep : existing) {
            if (dep.getDependsOnTaskId().equals(request.dependsOnTaskId())) {
                throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Dependency already exists");
            }
        }

        if (taskId.equals(request.dependsOnTaskId())) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Task cannot depend on itself");
        }

        TaskDependency dependency = new TaskDependency();
        dependency.setTaskId(taskId);
        dependency.setDependsOnTaskId(request.dependsOnTaskId());
        dependency.setDependencyType(request.dependencyType());
        dependency.setStatus(DependencyStatus.PENDING);
        TaskDependency saved = taskDependencyRepository.save(dependency);
        eventPublisher.publishEvent(new TaskDependencyCreatedEvent(toLong(taskId), toLong(request.dependsOnTaskId()), request.dependencyType().name()));
        return taskMapper.toDependencyResponse(saved);
    }

    @Override
    @Transactional
    public void removeDependency(UUID userId, UUID taskId, UUID dependsOnTaskId) {
        Task task = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Task not found"));
        TaskDependency dependency = taskDependencyRepository.findByTaskIdAndDependsOnTaskIdAndDeletedAtIsNull(taskId, dependsOnTaskId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Dependency not found"));
        dependency.setDeletedAt(Instant.now());
        taskDependencyRepository.save(dependency);
        eventPublisher.publishEvent(new TaskDependencyRemovedEvent(toLong(taskId), toLong(dependsOnTaskId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DependencyResponse> listDependencies(UUID userId, UUID taskId) {
        Task task = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Task not found"));
        return taskDependencyRepository.findByTaskIdAndDeletedAtIsNull(taskId)
                .stream().map(taskMapper::toDependencyResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DependencyResponse> listDependents(UUID userId, UUID taskId) {
        Task task = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Task not found"));
        return taskDependencyRepository.findByDependsOnTaskIdAndDeletedAtIsNull(taskId)
                .stream().map(taskMapper::toDependencyResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TimeEntryResponse startTimeEntry(UUID userId, UUID taskId, TaskService.StartTimeEntryRequest request) {
        Task task = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Task not found"));

        List<TaskTimeEntry> activeEntries = taskTimeEntryRepository.findByTaskIdAndUserIdAndDeletedAtIsNull(taskId, userId);
        for (TaskTimeEntry entry : activeEntries) {
            if (entry.getEndTime() == null) {
                throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Active time entry already exists for this task");
            }
        }

        TaskTimeEntry entry = new TaskTimeEntry();
        entry.setTaskId(taskId);
        entry.setUserId(userId);
        entry.setStartTime(Instant.now());
        entry.setEndTime(null);
        entry.setDurationMinutes(null);
        entry.setEntryType(request.entryType());
        entry.setNotes(request.notes());
        TaskTimeEntry saved = taskTimeEntryRepository.save(entry);
        eventPublisher.publishEvent(new TimeEntryStartedEvent(toLong(taskId), toLong(userId), java.time.LocalDateTime.now()));
        return taskMapper.toTimeEntryResponse(saved);
    }

    @Override
    @Transactional
    public TimeEntryResponse stopTimeEntry(UUID userId, UUID taskId, UUID entryId) {
        TaskTimeEntry entry = taskTimeEntryRepository.findById(entryId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Time entry not found"));
        if (!entry.getTaskId().equals(taskId) || !entry.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "Time entry not found");
        }
        if (entry.getEndTime() != null) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Time entry already stopped");
        }
        entry.setEndTime(Instant.now());
        long minutes = java.time.Duration.between(entry.getStartTime(), entry.getEndTime()).toMinutes();
        entry.setDurationMinutes((int) minutes);
        TaskTimeEntry saved = taskTimeEntryRepository.save(entry);
        eventPublisher.publishEvent(new TimeEntryStoppedEvent(toLong(taskId), toLong(userId), java.time.LocalDateTime.now(), saved.getDurationMinutes()));
        return taskMapper.toTimeEntryResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeEntryResponse> listTimeEntries(UUID userId, UUID taskId) {
        Task task = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Task not found"));
        return taskTimeEntryRepository.findByTaskIdAndDeletedAtIsNull(taskId)
                .stream().map(taskMapper::toTimeEntryResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteTimeEntry(UUID userId, UUID taskId, UUID entryId) {
        TaskTimeEntry entry = taskTimeEntryRepository.findById(entryId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Time entry not found"));
        if (!entry.getTaskId().equals(taskId) || !entry.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCodes.NOT_FOUND, "Time entry not found");
        }
        entry.setDeletedAt(Instant.now());
        taskTimeEntryRepository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskStatisticsResponse getStatistics(UUID userId) {
        List<Task> allTasks = taskRepository.findByUserIdAndDeletedAtIsNull(userId);
        long total = allTasks.size();
        long completed = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED).count();
        long failed = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.FAILED).count();
        long cancelled = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.CANCELLED).count();
        long archived = allTasks.stream().filter(t -> t.getStatus() == TaskStatus.ARCHIVED).count();

        Instant now = Instant.now();
        long overdue = allTasks.stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(now))
                .filter(t -> t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.CANCELLED && t.getStatus() != TaskStatus.ARCHIVED)
                .count();

        List<TaskTimeEntry> allEntries = taskTimeEntryRepository.findByUserIdAndDeletedAtIsNull(userId);
        long focusTimeToday = allEntries.stream()
                .filter(e -> e.getEndTime() != null)
                .filter(e -> isToday(e.getEndTime(), userId))
                .mapToLong(e -> e.getDurationMinutes() != null ? e.getDurationMinutes() : 0)
                .sum();
        long focusTimeWeek = allEntries.stream()
                .filter(e -> e.getEndTime() != null)
                .filter(e -> isThisWeek(e.getEndTime(), userId))
                .mapToLong(e -> e.getDurationMinutes() != null ? e.getDurationMinutes() : 0)
                .sum();

        double completionRate = total > 0 ? (double) completed / total * 100 : 0.0;

        Map<TaskStatus, Long> byStatus = new HashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            byStatus.put(status, allTasks.stream().filter(t -> t.getStatus() == status).count());
        }

        Map<TaskPriority, Long> byPriority = new HashMap<>();
        for (TaskPriority priority : TaskPriority.values()) {
            byPriority.put(priority, allTasks.stream().filter(t -> t.getPriority() == priority).count());
        }

        Map<String, Long> categoryBreakdown = new HashMap<>();
        for (Task task : allTasks) {
            String cat = task.getCategory() != null ? task.getCategory() : "uncategorized";
            categoryBreakdown.merge(cat, 1L, Long::sum);
        }

        return new TaskStatisticsResponse(
                completionRate, total, completed, failed, cancelled, archived, overdue,
                0.0, 0, focusTimeToday, focusTimeWeek, byStatus, byPriority, categoryBreakdown
        );
    }

    @Override
    @Transactional
    public RecurringConfigResponse configureRecurrence(UUID userId, UUID taskId, TaskService.UpdateRecurrenceRequest request) {
        Task task = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Task not found"));
        task.setIsRecurring(true);
        Task saved = taskRepository.save(task);

        RecurringTaskConfig config = new RecurringTaskConfig();
        config.setTaskId(taskId);
        config.setFrequency(request.frequency());
        config.setIntervalValue(request.intervalValue());
        config.setCronExpression(request.cronExpression());
        config.setDaysOfWeek(request.daysOfWeek() != null ? toJson(request.daysOfWeek()) : null);
        config.setDayOfMonth(request.dayOfMonth());
        config.setMonth(request.month());
        config.setExceptionDates(request.exceptionDates() != null ? toJson(request.exceptionDates()) : null);
        config.setEndDate(request.endDate());
        config.setMaxOccurrences(request.maxOccurrences());
        config.setOccurrenceCount(0);
        config.setIsActive(request.isActive() != null ? request.isActive() : true);
        RecurringTaskConfig savedConfig = recurringTaskConfigRepository.save(config);
        eventPublisher.publishEvent(new TaskRecurrenceGeneratedEvent(toLong(taskId), toLong(config.getId()), config.getOccurrenceCount()));
        return taskMapper.toRecurringConfigResponse(savedConfig);
    }

    @Override
    @Transactional
    public RecurringConfigResponse updateRecurrence(UUID userId, UUID taskId, TaskService.UpdateRecurrenceRequest request) {
        Task task = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Task not found"));
        RecurringTaskConfig config = recurringTaskConfigRepository.findByTaskId(taskId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Recurrence config not found"));
        config.setFrequency(request.frequency());
        config.setIntervalValue(request.intervalValue());
        config.setCronExpression(request.cronExpression());
        config.setDaysOfWeek(request.daysOfWeek() != null ? toJson(request.daysOfWeek()) : null);
        config.setDayOfMonth(request.dayOfMonth());
        config.setMonth(request.month());
        config.setExceptionDates(request.exceptionDates() != null ? toJson(request.exceptionDates()) : null);
        config.setEndDate(request.endDate());
        config.setMaxOccurrences(request.maxOccurrences());
        config.setIsActive(request.isActive() != null ? request.isActive() : true);
        RecurringTaskConfig saved = recurringTaskConfigRepository.save(config);
        return taskMapper.toRecurringConfigResponse(saved);
    }

    @Override
    @Transactional
    public void removeRecurrence(UUID userId, UUID taskId) {
        Task task = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Task not found"));
        List<RecurringTaskConfig> configs = recurringTaskConfigRepository.findByIsActive(true);
        List<RecurringTaskConfig> taskConfigs = configs.stream()
                .filter(c -> taskId.equals(c.getTaskId()))
                .collect(Collectors.toList());
        for (RecurringTaskConfig config : taskConfigs) {
            recurringTaskConfigRepository.delete(config);
        }
        task.setIsRecurring(false);
        taskRepository.save(task);
    }

    @Override
    @Transactional
    public TaskResponse addAttachment(UUID userId, UUID taskId, TaskService.AddAttachmentRequest request) {
        Task task = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Task not found"));
        List<Map<String, Object>> attachments = new ArrayList<>();
        try {
            if (task.getAttachments() != null && !task.getAttachments().isBlank()) {
                attachments = objectMapper.readValue(task.getAttachments(), new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            }
        } catch (Exception e) {
            attachments = new ArrayList<>();
        }
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("id", UUID.randomUUID().toString());
        attachment.put("type", request.type().name());
        attachment.put("name", request.name());
        attachment.put("url", request.url());
        attachment.put("fileId", request.fileId());
        attachment.put("sizeBytes", request.sizeBytes());
        attachment.put("mimeType", request.mimeType());
        attachments.add(attachment);
        task.setAttachments(toJson(attachments));
        Task saved = taskRepository.save(task);
        return taskMapper.toTaskResponse(saved);
    }

    @Override
    @Transactional
    public void removeAttachment(UUID userId, UUID taskId, UUID attachmentId) {
        Task task = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Task not found"));
        List<Map<String, Object>> attachments = new ArrayList<>();
        try {
            if (task.getAttachments() != null && !task.getAttachments().isBlank()) {
                attachments = objectMapper.readValue(task.getAttachments(), new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Failed to parse task attachments");
        }
        attachments.removeIf(a -> attachmentId.toString().equals(a.get("id")));
        task.setAttachments(toJson(attachments));
        taskRepository.save(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAttachments(UUID userId, UUID taskId) {
        Task task = taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Task not found"));
        try {
            if (task.getAttachments() != null && !task.getAttachments().isBlank()) {
                return objectMapper.readValue(task.getAttachments(), new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            }
        } catch (Exception e) {
            return List.of();
        }
        return List.of();
    }

    private void validateTransition(TaskStatus from, TaskStatus to) {
        boolean allowed = switch (from) {
            case DRAFT -> to == TaskStatus.PENDING || to == TaskStatus.IN_PROGRESS || to == TaskStatus.CANCELLED || to == TaskStatus.ARCHIVED;
            case PENDING -> to == TaskStatus.IN_PROGRESS || to == TaskStatus.CANCELLED || to == TaskStatus.ARCHIVED || to == TaskStatus.BLOCKED;
            case IN_PROGRESS -> to == TaskStatus.PAUSED || to == TaskStatus.COMPLETED || to == TaskStatus.FAILED || to == TaskStatus.BLOCKED || to == TaskStatus.CANCELLED || to == TaskStatus.ARCHIVED;
            case BLOCKED -> to == TaskStatus.IN_PROGRESS || to == TaskStatus.CANCELLED || to == TaskStatus.ARCHIVED;
            case PAUSED -> to == TaskStatus.IN_PROGRESS || to == TaskStatus.CANCELLED || to == TaskStatus.ARCHIVED;
            case COMPLETED -> to == TaskStatus.ARCHIVED;
            case FAILED -> to == TaskStatus.IN_PROGRESS || to == TaskStatus.CANCELLED || to == TaskStatus.ARCHIVED;
            case CANCELLED -> to == TaskStatus.ARCHIVED;
            case ARCHIVED -> to == TaskStatus.DRAFT;
        };
        if (!allowed) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                    "Invalid state transition from " + from + " to " + to);
        }
    }

    private void validateSubtasksCompleted(UUID taskId, UUID userId) {
        List<Task> subtasks = taskRepository.findByUserIdAndParentTaskIdAndDeletedAtIsNull(userId, taskId);
        for (Task subtask : subtasks) {
            if (subtask.getStatus() != TaskStatus.COMPLETED) {
                throw new BusinessException(ErrorCodes.VALIDATION_ERROR,
                        "Cannot complete task with incomplete subtask: " + subtask.getTitle());
            }
        }
    }

    private String toJson(Object obj) {
        try {
            return obj != null ? objectMapper.writeValueAsString(obj) : null;
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCodes.VALIDATION_ERROR, "Invalid JSON metadata");
        }
    }

    private Long toLong(UUID uuid) {
        return uuid != null ? uuid.getMostSignificantBits() : null;
    }

    private boolean isToday(Instant instant, UUID userId) {
        ZoneId zoneId = userTimezoneResolver.resolveUserZoneId(userId);
        LocalDate date = instant.atZone(zoneId).toLocalDate();
        return date.equals(LocalDate.now(zoneId));
    }

    private boolean isThisWeek(Instant instant, UUID userId) {
        ZoneId zoneId = userTimezoneResolver.resolveUserZoneId(userId);
        LocalDate date = instant.atZone(zoneId).toLocalDate();
        LocalDate now = LocalDate.now(zoneId);
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        return date.get(weekFields.weekOfYear()) == now.get(weekFields.weekOfYear()) &&
               date.get(weekFields.weekBasedYear()) == now.get(weekFields.weekBasedYear());
    }
}
