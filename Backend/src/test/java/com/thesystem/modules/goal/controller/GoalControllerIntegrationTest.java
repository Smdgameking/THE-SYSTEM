package com.thesystem.modules.goal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesystem.modules.goal.dto.CreateGoalRequest;
import com.thesystem.modules.goal.dto.GoalDetailResponse;
import com.thesystem.modules.goal.dto.GoalResponse;
import com.thesystem.modules.goal.dto.GoalStatisticsResponse;
import com.thesystem.modules.goal.dto.MilestoneResponse;
import com.thesystem.modules.goal.enums.GoalPriority;
import com.thesystem.modules.goal.enums.GoalStatus;
import com.thesystem.modules.goal.service.GoalService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class GoalControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {
        static GoalService goalService = Mockito.mock(GoalService.class);

        @Bean
        GoalService goalService() {
            return goalService;
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
    void shouldCreateGoal() throws Exception {
        UUID goalId = UUID.randomUUID();
        CreateGoalRequest request = new CreateGoalRequest(
                "Test Goal", "Description", "Category", GoalPriority.NORMAL,
                com.thesystem.modules.goal.enums.GoalDifficulty.EASY,
                com.thesystem.modules.goal.enums.GoalType.LONG_TERM,
                com.thesystem.modules.goal.enums.GoalVisibility.PRIVATE,
                100, Instant.now(), com.thesystem.modules.goal.enums.CompletionStrategy.MANUAL,
                List.of("tag1"), Map.of()
        );
        GoalResponse response = new GoalResponse(
                goalId, UUID.randomUUID(), "Test Goal", "Description", "Category",
                GoalPriority.NORMAL, com.thesystem.modules.goal.enums.GoalDifficulty.EASY,
                com.thesystem.modules.goal.enums.GoalStatus.DRAFT,
                com.thesystem.modules.goal.enums.GoalVisibility.PRIVATE,
                100, 0, 0.0, Instant.now(), null, null,
                com.thesystem.modules.goal.enums.CompletionStrategy.MANUAL,
                List.of("tag1"), Map.of(), Instant.now(), Instant.now()
        );

        Mockito.when(TestConfig.goalService.createGoal(any(UUID.class), any(CreateGoalRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Test Goal"));
    }

    @Test
    void shouldGetGoal() throws Exception {
        UUID goalId = UUID.randomUUID();
        GoalDetailResponse response = new GoalDetailResponse(
                goalId, UUID.randomUUID(), "Test Goal", "Description", "Category",
                GoalPriority.NORMAL, com.thesystem.modules.goal.enums.GoalDifficulty.EASY,
                com.thesystem.modules.goal.enums.GoalStatus.DRAFT,
                com.thesystem.modules.goal.enums.GoalVisibility.PRIVATE,
                100, 0, 0.0, Instant.now(), null, null,
                com.thesystem.modules.goal.enums.CompletionStrategy.MANUAL,
                List.of("tag1"), Map.of(), Instant.now(), Instant.now(), List.of()
        );

        Mockito.when(TestConfig.goalService.getGoal(any(UUID.class), any(UUID.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/goals/" + goalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Test Goal"));
    }

    @Test
    void shouldStartGoal() throws Exception {
        UUID goalId = UUID.randomUUID();
        GoalResponse response = new GoalResponse(
                goalId, UUID.randomUUID(), "Test Goal", "Description", "Category",
                GoalPriority.NORMAL, com.thesystem.modules.goal.enums.GoalDifficulty.EASY,
                com.thesystem.modules.goal.enums.GoalStatus.ACTIVE,
                com.thesystem.modules.goal.enums.GoalVisibility.PRIVATE,
                100, 0, 0.0, Instant.now(), null, null,
                com.thesystem.modules.goal.enums.CompletionStrategy.MANUAL,
                List.of("tag1"), Map.of(), Instant.now(), Instant.now()
        );

        Mockito.when(TestConfig.goalService.startGoal(any(UUID.class), any(UUID.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/goals/" + goalId + "/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void shouldCompleteGoal() throws Exception {
        UUID goalId = UUID.randomUUID();
        GoalResponse response = new GoalResponse(
                goalId, UUID.randomUUID(), "Test Goal", "Description", "Category",
                GoalPriority.NORMAL, com.thesystem.modules.goal.enums.GoalDifficulty.EASY,
                com.thesystem.modules.goal.enums.GoalStatus.COMPLETED,
                com.thesystem.modules.goal.enums.GoalVisibility.PRIVATE,
                100, 100, 100.0, Instant.now(), Instant.now(), null,
                com.thesystem.modules.goal.enums.CompletionStrategy.MANUAL,
                List.of("tag1"), Map.of(), Instant.now(), Instant.now()
        );

        Mockito.when(TestConfig.goalService.completeGoal(any(UUID.class), any(UUID.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/goals/" + goalId + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void shouldCreateMilestone() throws Exception {
        UUID goalId = UUID.randomUUID();
        MilestoneResponse response = new MilestoneResponse(
                UUID.randomUUID(), goalId, "Milestone 1", "Description", 0, false, null, Instant.now(), Instant.now()
        );

        Mockito.when(TestConfig.goalService.createMilestone(any(UUID.class), any(UUID.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/goals/" + goalId + "/milestones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new com.thesystem.modules.goal.dto.CreateMilestoneRequest("Milestone 1", "Description", 0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Milestone 1"));
    }

    @Test
    void shouldGetStatistics() throws Exception {
        GoalStatisticsResponse response = new GoalStatisticsResponse(
                10, 5, 3, 1, 1, 75.5,
                Map.of(GoalStatus.ACTIVE, 5L, GoalStatus.COMPLETED, 3L),
                Map.of(GoalPriority.NORMAL, 8L, GoalPriority.HIGH, 2L)
        );

        Mockito.when(TestConfig.goalService.getStatistics(any(UUID.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/goals/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalGoals").value(10));
    }
}
