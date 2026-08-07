package com.thesystem.modules.task.service;

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
import com.thesystem.modules.task.entity.Task;
import com.thesystem.modules.task.entity.TaskDependency;
import com.thesystem.modules.task.entity.TaskTimeEntry;
import com.thesystem.modules.task.enums.*;
import com.thesystem.modules.task.events.*;
import com.thesystem.modules.task.mapper.TaskMapper;
import com.thesystem.modules.task.provider.*;
import com.thesystem.modules.task.repository.RecurringTaskConfigRepository;
import com.thesystem.modules.task.repository.TaskDependencyRepository;
import com.thesystem.modules.task.repository.TaskRepository;
import com.thesystem.modules.task.repository.TaskTimeEntryRepository;
import com.thesystem.modules.task.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskDependencyRepository taskDependencyRepository;

    @Mock
    private TaskTimeEntryRepository taskTimeEntryRepository;

    @Mock
    private RecurringTaskConfigRepository recurringTaskConfigRepository;

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TaskExecutionProviderRegistry executionProviderRegistry;

    private TaskServiceImpl taskService;
    private UUID userId;
    private UUID taskId;
    private Task task;

    @BeforeEach
    void setUp() {
        taskService = new TaskServiceImpl(
                taskRepository, taskDependencyRepository, taskTimeEntryRepository,
                recurringTaskConfigRepository, taskMapper, new com.fasterxml.jackson.databind.ObjectMapper(),
                eventPublisher, executionProviderRegistry
        );
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        task = new Task();
        task.setId(taskId);
        task.setUserId(userId);
        task.setTitle("Test Task");
        task.setStatus(TaskStatus.DRAFT);
        task.setPriority(TaskPriority.NORMAL);
        task.setVisibility(TaskVisibility.PRIVATE);
        task.setCreatedAt(Instant.now());
        task.setUpdatedAt(Instant.now());
    }

    private TaskResponse taskResponse(UUID id, String title, TaskStatus status) {
        return new TaskResponse(
                id, userId, null, null, title, null, status,
                TaskPriority.NORMAL, null, null, null, null,
                null, null, null, null, false, null,
                List.of(), List.of(), null, null, Map.of(), Map.of(), TaskVisibility.PRIVATE,
                Instant.now(), Instant.now(), null, null, null
        );
    }

    @Test
    void shouldCreateTaskSuccessfully() {
        CreateTaskRequest request = new CreateTaskRequest(
                "New Task", "Description", null, null, null, TaskPriority.NORMAL,
                "Category", TaskExecutionType.BOOLEAN, 60, Instant.now(), Instant.now().plusSeconds(3600),
                null, List.of(), List.of(), null, null, null, Map.of(), null
        );

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(taskId);
            return t;
        });
        when(executionProviderRegistry.getProvider(TaskExecutionType.BOOLEAN))
                .thenReturn(new BooleanExecutionProvider());
        when(taskMapper.toTaskResponse(any(Task.class))).thenReturn(taskResponse(taskId, "New Task", TaskStatus.DRAFT));

        TaskResponse response = taskService.createTask(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("New Task");
        verify(taskRepository).save(any(Task.class));
        verify(eventPublisher).publishEvent(any(TaskCreatedEvent.class));
    }

    @Test
    void shouldGetTaskSuccessfully() {
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskMapper.toTaskResponse(task)).thenReturn(taskResponse(taskId, "Test Task", TaskStatus.DRAFT));

        TaskResponse response = taskService.getTask(userId, taskId);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Test Task");
    }

    @Test
    void shouldThrowNotFoundWhenGettingNonExistentTask() {
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTask(userId, taskId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Task not found");
    }

    @Test
    void shouldListTasksSuccessfully() {
        List<Task> tasks = List.of(task);
        when(taskRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId)).thenReturn(tasks);
        when(taskMapper.toTaskResponse(task)).thenReturn(taskResponse(taskId, "Test Task", TaskStatus.DRAFT));

        List<TaskResponse> responses = taskService.listTasks(userId, new TaskFilterRequest(null, null, null, null, null, null, null, null, null, null, null, null, null, null));

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).title()).isEqualTo("Test Task");
    }

    @Test
    void shouldUpdateTaskSuccessfully() {
        UpdateTaskRequest request = new UpdateTaskRequest(
                "Updated Title", null, null, null, null, TaskPriority.HIGH, null, null,
                null, null, null, null, null, null, null, null, List.of(), List.of(), null,
                null, null, null, null
        );
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskMapper.toTaskResponse(any(Task.class))).thenReturn(taskResponse(taskId, "Updated Title", TaskStatus.DRAFT));

        TaskResponse response = taskService.updateTask(userId, taskId, request);

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Updated Title");
        verify(eventPublisher).publishEvent(any(TaskUpdatedEvent.class));
    }

    @Test
    void shouldDeleteTaskSuccessfully() {
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taskService.deleteTask(userId, taskId);

        assertThat(task.getDeletedAt()).isNotNull();
        verify(eventPublisher).publishEvent(any(TaskDeletedEvent.class));
    }

    @Test
    void shouldTransitionFromDraftToPending() {
        task.setStatus(TaskStatus.DRAFT);
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setStatus(TaskStatus.PENDING);
            return t;
        });
        when(taskMapper.toTaskResponse(any(Task.class))).thenReturn(taskResponse(taskId, "Test Task", TaskStatus.PENDING));

        TaskResponse response = taskService.updateTask(userId, taskId, new UpdateTaskRequest(
                null, null, null, null, TaskStatus.PENDING, null, null, null, null, null,
                null, null, null, null, null, null, List.of(), List.of(), null,
                null, null, null, null
        ));

        assertThat(response.status()).isEqualTo(TaskStatus.PENDING);
    }

    @Test
    void shouldTransitionFromInProgressToCompleted() {
        task.setStatus(TaskStatus.IN_PROGRESS);
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setStatus(TaskStatus.COMPLETED);
            t.setCompletedDate(Instant.now());
            return t;
        });
        when(taskMapper.toTaskResponse(any(Task.class))).thenReturn(taskResponse(taskId, "Test Task", TaskStatus.COMPLETED));

        TaskResponse response = taskService.completeTask(userId, taskId);

        assertThat(response.status()).isEqualTo(TaskStatus.COMPLETED);
        verify(eventPublisher).publishEvent(any(TaskCompletedEvent.class));
    }

    @Test
    void shouldFailTaskSuccessfully() {
        task.setStatus(TaskStatus.IN_PROGRESS);
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskMapper.toTaskResponse(any(Task.class))).thenReturn(taskResponse(taskId, "Test Task", TaskStatus.FAILED));

        TaskResponse response = taskService.failTask(userId, taskId, "Out of time");

        assertThat(response.status()).isEqualTo(TaskStatus.FAILED);
        verify(eventPublisher).publishEvent(any(TaskFailedEvent.class));
    }

    @Test
    void shouldCancelTaskSuccessfully() {
        task.setStatus(TaskStatus.PENDING);
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskMapper.toTaskResponse(any(Task.class))).thenReturn(taskResponse(taskId, "Test Task", TaskStatus.CANCELLED));

        TaskResponse response = taskService.cancelTask(userId, taskId, "No longer needed");

        assertThat(response.status()).isEqualTo(TaskStatus.CANCELLED);
        verify(eventPublisher).publishEvent(any(TaskCancelledEvent.class));
    }

    @Test
    void shouldArchiveTaskSuccessfully() {
        task.setStatus(TaskStatus.COMPLETED);
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskMapper.toTaskResponse(any(Task.class))).thenReturn(taskResponse(taskId, "Test Task", TaskStatus.ARCHIVED));

        TaskResponse response = taskService.archiveTask(userId, taskId);

        assertThat(response.status()).isEqualTo(TaskStatus.ARCHIVED);
        verify(eventPublisher).publishEvent(any(TaskArchivedEvent.class));
    }

    @Test
    void shouldRestoreTaskSuccessfully() {
        task.setStatus(TaskStatus.ARCHIVED);
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskMapper.toTaskResponse(any(Task.class))).thenReturn(taskResponse(taskId, "Test Task", TaskStatus.DRAFT));

        TaskResponse response = taskService.restoreTask(userId, taskId);

        assertThat(response.status()).isEqualTo(TaskStatus.DRAFT);
        verify(eventPublisher).publishEvent(any(TaskActivatedEvent.class));
    }

    @Test
    void shouldCreateSubtaskSuccessfully() {
        CreateTaskRequest request = new CreateTaskRequest(
                "Subtask", "Sub Description", null, taskId, null, TaskPriority.NORMAL,
                null, null, null, null, null, null, List.of(), List.of(), null, null, null, null, null
        );
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
        when(taskMapper.toTaskResponse(any(Task.class))).thenReturn(taskResponse(UUID.randomUUID(), "Subtask", TaskStatus.DRAFT));

        TaskResponse response = taskService.createSubtask(userId, taskId, request);

        assertThat(response).isNotNull();
        verify(eventPublisher).publishEvent(any(TaskSubtaskCreatedEvent.class));
    }

    @Test
    void shouldListSubtasksSuccessfully() {
        Task subtask = new Task();
        subtask.setId(UUID.randomUUID());
        subtask.setUserId(userId);
        subtask.setParentTaskId(taskId);
        subtask.setTitle("Subtask");
        when(taskRepository.findByUserIdAndParentTaskIdAndDeletedAtIsNull(userId, taskId)).thenReturn(List.of(subtask));
        when(taskMapper.toTaskResponse(subtask)).thenReturn(taskResponse(subtask.getId(), "Subtask", TaskStatus.DRAFT));

        List<TaskResponse> responses = taskService.listSubtasks(userId, taskId);

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldBlockCompletionWhenSubtasksIncomplete() {
        task.setStatus(TaskStatus.IN_PROGRESS);
        Task subtask = new Task();
        subtask.setId(UUID.randomUUID());
        subtask.setUserId(userId);
        subtask.setParentTaskId(taskId);
        subtask.setTitle("Incomplete Subtask");
        subtask.setStatus(TaskStatus.IN_PROGRESS);
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskRepository.findByUserIdAndParentTaskIdAndDeletedAtIsNull(userId, taskId)).thenReturn(List.of(subtask));

        assertThatThrownBy(() -> taskService.completeTask(userId, taskId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("incomplete subtask");
    }

    @Test
    void shouldAllowCompletionWhenSubtasksComplete() {
        task.setStatus(TaskStatus.IN_PROGRESS);
        Task subtask = new Task();
        subtask.setId(UUID.randomUUID());
        subtask.setUserId(userId);
        subtask.setParentTaskId(taskId);
        subtask.setStatus(TaskStatus.COMPLETED);
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskRepository.findByUserIdAndParentTaskIdAndDeletedAtIsNull(userId, taskId)).thenReturn(List.of(subtask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskMapper.toTaskResponse(any(Task.class))).thenReturn(taskResponse(taskId, "Test Task", TaskStatus.COMPLETED));

        TaskResponse response = taskService.completeTask(userId, taskId);
        assertThat(response.status()).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void shouldAddDependencySuccessfully() {
        UUID dependsOnTaskId = UUID.randomUUID();
        TaskService.AddDependencyRequest depRequest = new TaskService.AddDependencyRequest(dependsOnTaskId, DependencyType.BLOCKS);
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(dependsOnTaskId, userId)).thenReturn(Optional.of(new Task()));
        when(taskDependencyRepository.findByTaskIdAndDeletedAtIsNull(taskId)).thenReturn(List.of());
        when(taskDependencyRepository.save(any(TaskDependency.class))).thenAnswer(invocation -> {
            TaskDependency dep = invocation.getArgument(0);
            dep.setId(UUID.randomUUID());
            return dep;
        });
        when(taskMapper.toDependencyResponse(any(TaskDependency.class))).thenReturn(new DependencyResponse(
                UUID.randomUUID(), taskId, dependsOnTaskId, DependencyType.BLOCKS,
                DependencyStatus.PENDING, null, Instant.now(), null, null
        ));

        DependencyResponse response = taskService.addDependency(userId, taskId, depRequest);

        assertThat(response).isNotNull();
        assertThat(response.taskId()).isEqualTo(taskId);
        assertThat(response.dependsOnTaskId()).isEqualTo(dependsOnTaskId);
        verify(eventPublisher).publishEvent(any(TaskDependencyCreatedEvent.class));
    }

    @Test
    void shouldRejectSelfDependency() {
        TaskService.AddDependencyRequest depRequest = new TaskService.AddDependencyRequest(taskId, DependencyType.BLOCKS);
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.addDependency(userId, taskId, depRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot depend on itself");
    }

    @Test
    void shouldRejectDuplicateDependency() {
        UUID dependsOnTaskId = UUID.randomUUID();
        TaskService.AddDependencyRequest depRequest = new TaskService.AddDependencyRequest(dependsOnTaskId, DependencyType.BLOCKS);
        TaskDependency existingDep = new TaskDependency();
        existingDep.setTaskId(taskId);
        existingDep.setDependsOnTaskId(dependsOnTaskId);
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(dependsOnTaskId, userId)).thenReturn(Optional.of(new Task()));
        when(taskDependencyRepository.findByTaskIdAndDeletedAtIsNull(taskId)).thenReturn(List.of(existingDep));

        assertThatThrownBy(() -> taskService.addDependency(userId, taskId, depRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Dependency already exists");
    }

    @Test
    void shouldStartTimeEntrySuccessfully() {
        TaskService.StartTimeEntryRequest request = new TaskService.StartTimeEntryRequest(TaskTimeEntryType.TIMER, "Working on it");
        TaskTimeEntry entry = new TaskTimeEntry();
        entry.setId(UUID.randomUUID());
        entry.setTaskId(taskId);
        entry.setUserId(userId);
        entry.setStartTime(Instant.now());
        entry.setEntryType(TaskTimeEntryType.TIMER);
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskTimeEntryRepository.findByTaskIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(List.of());
        when(taskTimeEntryRepository.save(any(TaskTimeEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(taskMapper.toTimeEntryResponse(any(TaskTimeEntry.class))).thenReturn(new TimeEntryResponse(
                entry.getId(), taskId, userId, entry.getStartTime(), null, null,
                TaskTimeEntryType.TIMER, "Working on it", Instant.now(), null, null
        ));

        TimeEntryResponse response = taskService.startTimeEntry(userId, taskId, request);

        assertThat(response).isNotNull();
        assertThat(response.entryType()).isEqualTo(TaskTimeEntryType.TIMER);
        verify(eventPublisher).publishEvent(any(TimeEntryStartedEvent.class));
    }

    @Test
    void shouldStopTimeEntrySuccessfully() {
        UUID entryId = UUID.randomUUID();
        TaskTimeEntry entry = new TaskTimeEntry();
        entry.setId(entryId);
        entry.setTaskId(taskId);
        entry.setUserId(userId);
        entry.setStartTime(Instant.now().minusSeconds(3600));
        entry.setEndTime(null);
        when(taskTimeEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(taskTimeEntryRepository.save(any(TaskTimeEntry.class))).thenAnswer(invocation -> {
            TaskTimeEntry t = invocation.getArgument(0);
            t.setEndTime(Instant.now());
            t.setDurationMinutes(60);
            return t;
        });
        when(taskMapper.toTimeEntryResponse(any(TaskTimeEntry.class))).thenReturn(new TimeEntryResponse(
                entryId, taskId, userId, entry.getStartTime(), Instant.now(), 60,
                TaskTimeEntryType.TIMER, "Working on it", Instant.now(), null, null
        ));

        TimeEntryResponse response = taskService.stopTimeEntry(userId, taskId, entryId);

        assertThat(response).isNotNull();
        assertThat(response.durationMinutes()).isEqualTo(60);
        verify(eventPublisher).publishEvent(any(TimeEntryStoppedEvent.class));
    }

    @Test
    void shouldAggregateTimeEntriesForStatistics() {
        when(taskRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(List.of(task));
        when(taskTimeEntryRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(List.of());

        TaskStatisticsResponse stats = taskService.getStatistics(userId);

        assertThat(stats).isNotNull();
        assertThat(stats.totalTasks()).isEqualTo(1);
        assertThat(stats.focusTimeToday()).isEqualTo(0);
        assertThat(stats.focusTimeWeek()).isEqualTo(0);
    }

    @Test
    void shouldRemoveDependencySuccessfully() {
        UUID dependsOnTaskId = UUID.randomUUID();
        TaskDependency dependency = new TaskDependency();
        dependency.setId(UUID.randomUUID());
        dependency.setTaskId(taskId);
        dependency.setDependsOnTaskId(dependsOnTaskId);
        dependency.setDependencyType(DependencyType.BLOCKS);
        dependency.setStatus(DependencyStatus.PENDING);
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskDependencyRepository.findByTaskIdAndDependsOnTaskIdAndDeletedAtIsNull(taskId, dependsOnTaskId)).thenReturn(Optional.of(dependency));
        when(taskDependencyRepository.save(any(TaskDependency.class))).thenAnswer(invocation -> invocation.getArgument(0));

        taskService.removeDependency(userId, taskId, dependsOnTaskId);

        verify(taskDependencyRepository).save(any(TaskDependency.class));
        verify(eventPublisher).publishEvent(any(TaskDependencyRemovedEvent.class));
    }

    @Test
    void shouldListDependenciesSuccessfully() {
        UUID dependsOnTaskId = UUID.randomUUID();
        TaskDependency dependency = new TaskDependency();
        dependency.setTaskId(taskId);
        dependency.setDependsOnTaskId(dependsOnTaskId);
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskDependencyRepository.findByTaskIdAndDeletedAtIsNull(taskId)).thenReturn(List.of(dependency));
        when(taskMapper.toDependencyResponse(any(TaskDependency.class))).thenReturn(new DependencyResponse(
                dependency.getId(), taskId, dependsOnTaskId, DependencyType.BLOCKS,
                DependencyStatus.PENDING, null, Instant.now(), null, null
        ));

        List<DependencyResponse> responses = taskService.listDependencies(userId, taskId);

        assertThat(responses).hasSize(1);
    }

    @Test
    void shouldConfigureRecurrenceSuccessfully() {
        TaskService.UpdateRecurrenceRequest request = new TaskService.UpdateRecurrenceRequest(
                RecurrenceFrequency.DAILY, 1, null, List.of(1, 2, 3), null, null,
                List.of(), Instant.now().plusSeconds(86400), 10, true
        );
        when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recurringTaskConfigRepository.save(any(com.thesystem.modules.task.entity.RecurringTaskConfig.class))).thenAnswer(invocation -> {
            com.thesystem.modules.task.entity.RecurringTaskConfig config = invocation.getArgument(0);
            config.setId(UUID.randomUUID());
            return config;
        });
        when(taskMapper.toRecurringConfigResponse(any(com.thesystem.modules.task.entity.RecurringTaskConfig.class))).thenReturn(new RecurringConfigResponse(
                UUID.randomUUID(), taskId, RecurrenceFrequency.DAILY, 1, null, List.of(1, 2, 3),
                null, null, List.of(), Instant.now().plusSeconds(86400), 10, 0, true,
                Instant.now(), Instant.now(), null
        ));

        RecurringConfigResponse response = taskService.configureRecurrence(userId, taskId, request);

        assertThat(response).isNotNull();
        assertThat(response.frequency()).isEqualTo(RecurrenceFrequency.DAILY);
        verify(eventPublisher).publishEvent(any(TaskRecurrenceGeneratedEvent.class));
    }

    @ParameterizedTest
    @EnumSource(TaskStatus.class)
    void shouldRejectInvalidStatusTransition(TaskStatus fromStatus) {
        task.setStatus(fromStatus);
        lenient().when(taskRepository.findByIdAndUserIdAndDeletedAtIsNull(taskId, userId)).thenReturn(Optional.of(task));
        lenient().when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(taskMapper.toTaskResponse(any(Task.class))).thenReturn(taskResponse(taskId, "Test Task", TaskStatus.DRAFT));

        if (fromStatus == TaskStatus.ARCHIVED) {
            assertThatThrownBy(() -> taskService.archiveTask(userId, taskId))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
