package com.thesystem.modules.analytics.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesystem.modules.analytics.dto.AnalyticsTrendsResponse;
import com.thesystem.modules.analytics.dto.AiUsageAnalyticsResponse;
import com.thesystem.modules.analytics.dto.AnalyticsOverviewResponse;
import com.thesystem.modules.analytics.dto.MemoryUsageAnalyticsResponse;
import com.thesystem.modules.analytics.service.AnalyticsService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AnalyticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {
        static AnalyticsService analyticsService = Mockito.mock(AnalyticsService.class);

        @Bean
        AnalyticsService analyticsService() {
            return analyticsService;
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
    void shouldReturnOverview() throws Exception {
        UUID accountId = UUID.randomUUID();
        var xpAccount = new com.thesystem.modules.xp.dto.xpaccount.XpAccountResponse(
                accountId, UUID.randomUUID(), 100, 5, 200, 0, 500, 0.5,
                Instant.now(), Instant.now()
        );
        var streak = new com.thesystem.modules.xp.dto.streak.UserStreakResponse(7, 14, null, null);
        var taskStats = new com.thesystem.modules.task.dto.TaskStatisticsResponse(
                0.5, 10L, 5L, 3L, 2L, 1L, 0L, 0.0, 0L, 0L, 0L,
                Map.of(), Map.of(), Map.of()
        );
        var goalStats = new com.thesystem.modules.goal.dto.GoalStatisticsResponse(
                4L, 2L, 1L, 1L, 0L, 50.0,
                Map.of(), Map.of()
        );
        var memoryUsage = new com.thesystem.modules.analytics.dto.MemoryUsageAnalyticsResponse(
                3L, Map.of(), Map.of(), Map.of(), List.of()
        );
        var aiUsage = new com.thesystem.modules.analytics.dto.AiUsageAnalyticsResponse(
                5L, 100L, 60L, 40L, 20.0, Map.of(), Map.of(), List.of()
        );
        var overview = new com.thesystem.modules.analytics.dto.AnalyticsOverviewResponse(
                xpAccount, streak, taskStats, goalStats, memoryUsage, aiUsage
        );

        Mockito.when(TestConfig.analyticsService.getOverview(any(UUID.class)))
                .thenReturn(overview);

        mockMvc.perform(get("/api/v1/analytics/overview")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.xpAccount.currentXp").value(100))
                .andExpect(jsonPath("$.data.streak.currentStreak").value(7));
    }

    @Test
    void shouldReturnTrends() throws Exception {
        var trends = new AnalyticsTrendsResponse(
                14, Instant.now().atZone(java.time.ZoneId.systemDefault()).toLocalDate().minusDays(13),
                Instant.now().atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
                List.of(), List.of(), List.of(), List.of(), List.of(), Map.of()
        );

        Mockito.when(TestConfig.analyticsService.getTrends(any(UUID.class), any(Integer.class)))
                .thenReturn(trends);

        mockMvc.perform(get("/api/v1/analytics/trends?days=14")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.days").value(14));
    }

    @Test
    void shouldRejectInvalidDays() throws Exception {
        UUID userId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, null)
        );
        Mockito.when(TestConfig.analyticsService.getTrends(any(UUID.class), org.mockito.ArgumentMatchers.eq(0)))
                .thenThrow(new com.thesystem.common.exception.BusinessException(
                        com.thesystem.common.constants.ErrorCodes.BAD_REQUEST, "days must be between 1 and 90"));

        mockMvc.perform(get("/api/v1/analytics/trends?days=0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldReturnAiUsage() throws Exception {
        var aiUsage = new AiUsageAnalyticsResponse(
                0L, 0L, 0L, 0L, 0.0, Map.of(), Map.of(), List.of()
        );

        Mockito.when(TestConfig.analyticsService.getAiUsage(any(UUID.class)))
                .thenReturn(aiUsage);

        mockMvc.perform(get("/api/v1/analytics/ai-usage")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalInteractions").value(0));
    }

    @Test
    void shouldReturnMemoryUsage() throws Exception {
        var memoryUsage = new MemoryUsageAnalyticsResponse(
                0L, Map.of(), Map.of(), Map.of(), List.of()
        );

        Mockito.when(TestConfig.analyticsService.getMemoryUsage(any(UUID.class)))
                .thenReturn(memoryUsage);

        mockMvc.perform(get("/api/v1/analytics/memories")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalMemories").value(0));
    }
}
