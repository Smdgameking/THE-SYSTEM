package com.thesystem.modules.task.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesystem.modules.task.dto.CreateTaskRequest;
import com.thesystem.modules.task.dto.DependencyResponse;
import com.thesystem.modules.task.dto.RecurringConfigResponse;
import com.thesystem.modules.task.dto.TaskResponse;
import com.thesystem.modules.task.dto.TaskStatisticsResponse;
import com.thesystem.modules.task.dto.TimeEntryResponse;
import com.thesystem.modules.task.dto.UpdateTaskRequest;
import com.thesystem.modules.task.enums.DependencyStatus;
import com.thesystem.modules.task.enums.DependencyType;
import com.thesystem.modules.task.enums.RecurrenceFrequency;
import com.thesystem.modules.task.enums.TaskExecutionType;
import com.thesystem.modules.task.enums.TaskPriority;
import com.thesystem.modules.task.enums.TaskStatus;
import com.thesystem.modules.task.enums.TaskTimeEntryType;
import com.thesystem.modules.task.enums.TaskVisibility;
import com.thesystem.modules.task.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {
        static TaskService taskService = Mockito.mock(TaskService.class);

        @Bean
        TaskService taskService() {
            return taskService;
        }
    }

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, null)
        );
    }

    @Test
    void shouldCreateTask() throws Exception {
        UUID taskId = UUID.randomUUID();
        CreateTaskRequest request = new CreateTaskRequest(
                "Test Task", "Description", null, null,
                TaskStatus.DRAFT, TaskPriority.NORMAL, null, "Category",
                TaskExecutionType.BOOLEAN, 60, Instant.now(), Instant.now().plusSeconds(3600), null,
                List.of("tag1"), List.of(), "Notes", Map.of(), Map.of(), Map.of(),
                TaskVisibility.PRIVATE
        );
        TaskResponse response = new TaskResponse(
                taskId, UUID.randomUUID(), null, null, "Test Task", "Description",
                TaskStatus.DRAFT, TaskPriority.NORMAL, null, "Category",
                TaskExecutionType.BOOLEAN, 60, null,
                Instant.now(), Instant.now().plusSeconds(3600), null, null,
                false, null, List.of("tag1"), List.of(), "Notes",
                Map.of(), Map.of(), Map.of(),
                TaskVisibility.PRIVATE, Instant.now(), Instant.now(), UUID.randomUUID(), UUID.randomUUID(), null
        );

        Mockito.when(TestConfig.taskService.createTask(any(UUID.class), any(CreateTaskRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Test Task"));
    }

    @Test
    void shouldGetTask() throws Exception {
        UUID taskId = UUID.randomUUID();
        TaskResponse response = new TaskResponse(
                taskId, UUID.randomUUID(), null, null, "Test Task", "Description",
                TaskStatus.DRAFT, TaskPriority.NORMAL, null, "Category",
                TaskExecutionType.BOOLEAN, 60, null,
                Instant.now(), Instant.now().plusSeconds(3600), null, null,
                false, null, List.of(), List.of(), "Notes",
                Map.of(), Map.of(), Map.of(),
                TaskVisibility.PRIVATE, Instant.now(), Instant.now(), UUID.randomUUID(), UUID.randomUUID(), null
        );

        Mockito.when(TestConfig.taskService.getTask(any(UUID.class), any(UUID.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/tasks/" + taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Test Task"));
    }

    @Test
    void shouldListTasks() throws Exception {
        UUID taskId = UUID.randomUUID();
        TaskResponse response = new TaskResponse(
                taskId, UUID.randomUUID(), null, null, "Test Task", "Description",
                TaskStatus.DRAFT, TaskPriority.NORMAL, null, "Category",
                TaskExecutionType.BOOLEAN, 60, null,
                Instant.now(), Instant.now().plusSeconds(3600), null, null,
                false, null, List.of(), List.of(), "Notes",
                Map.of(), Map.of(), Map.of(),
                TaskVisibility.PRIVATE, Instant.now(), Instant.now(), UUID.randomUUID(), UUID.randomUUID(), null
        );

        Mockito.when(TestConfig.taskService.listTasks(any(UUID.class), any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("Test Task"));
    }

    @Test
    void shouldRejectCreateTaskWithBlankTitle() throws Exception {
        String body = """
                {"title": "", "priority": "NORMAL"}
                """;

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectCreateTaskWithOversizedDescription() throws Exception {
        String oversized = "x".repeat(5001);
        String body = """
                {"title": "Valid Task", "description": "%s"}
                """.formatted(oversized);

        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldRejectUpdateTaskWithBlankTitle() throws Exception {
        UUID taskId = UUID.randomUUID();
        String body = """
                {"title": ""}
                """;

        mockMvc.perform(patch("/api/v1/tasks/" + taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldCompleteTask() throws Exception {
        UUID taskId = UUID.randomUUID();
        TaskResponse response = new TaskResponse(
                taskId, UUID.randomUUID(), null, null, "Test Task", "Description",
                TaskStatus.COMPLETED, TaskPriority.NORMAL, null, "Category",
                TaskExecutionType.BOOLEAN, 60, null,
                Instant.now(), Instant.now().plusSeconds(3600), Instant.now(), null,
                false, null, List.of(), List.of(), "Notes",
                Map.of(), Map.of(), Map.of(),
                TaskVisibility.PRIVATE, Instant.now(), Instant.now(), UUID.randomUUID(), UUID.randomUUID(), null
        );

        Mockito.when(TestConfig.taskService.completeTask(any(UUID.class), any(UUID.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void shouldAddDependency() throws Exception {
        UUID taskId = UUID.randomUUID();
        DependencyResponse response = new DependencyResponse(
                UUID.randomUUID(), taskId, UUID.randomUUID(), DependencyType.BLOCKS, DependencyStatus.PENDING, null, Instant.now(), UUID.randomUUID(), null
        );

        TaskService.AddDependencyRequest request = new TaskService.AddDependencyRequest(
                UUID.randomUUID(), DependencyType.BLOCKS
        );

        Mockito.when(TestConfig.taskService.addDependency(any(UUID.class), any(UUID.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/dependencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldStartTimeEntry() throws Exception {
        UUID taskId = UUID.randomUUID();
        TimeEntryResponse response = new TimeEntryResponse(
                UUID.randomUUID(), taskId, UUID.randomUUID(), Instant.now(), null, null, TaskTimeEntryType.TIMER, "Notes", Instant.now(), UUID.randomUUID(), null
        );

        TaskService.StartTimeEntryRequest request = new TaskService.StartTimeEntryRequest(
                TaskTimeEntryType.TIMER, "Focus session"
        );

        Mockito.when(TestConfig.taskService.startTimeEntry(any(UUID.class), any(UUID.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/time-entries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldGetStatistics() throws Exception {
        TaskStatisticsResponse response = new TaskStatisticsResponse(
                50.0, 10L, 5L, 1L, 0L, 2L, 2L, 3.5, 5L, 120L, 240L,
                Map.of(TaskStatus.COMPLETED, 5L, TaskStatus.IN_PROGRESS, 3L),
                Map.of(TaskPriority.NORMAL, 7L, TaskPriority.HIGH, 3L),
                Map.of("Work", 8L, "Personal", 2L)
        );

        Mockito.when(TestConfig.taskService.getStatistics(any(UUID.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/tasks/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalTasks").value(10));
    }

    @Test
    void shouldConfigureRecurrence() throws Exception {
        UUID taskId = UUID.randomUUID();
        RecurringConfigResponse response = new RecurringConfigResponse(
                UUID.randomUUID(), taskId, RecurrenceFrequency.DAILY, 1,
                null, null, null, null, null, null, 0, 0, true,
                Instant.now(), Instant.now(), UUID.randomUUID()
        );

        TaskService.UpdateRecurrenceRequest request = new TaskService.UpdateRecurrenceRequest(
                RecurrenceFrequency.DAILY, 1, null, null, null, null, null, null, null, true
        );

        Mockito.when(TestConfig.taskService.configureRecurrence(any(UUID.class), any(UUID.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/tasks/" + taskId + "/recurrence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.frequency").value("DAILY"));
    }
}
