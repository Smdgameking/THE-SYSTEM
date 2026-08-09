package com.thesystem.modules.xp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesystem.modules.xp.dto.achievement.AchievementResponse;
import com.thesystem.modules.xp.dto.achievement.UserAchievementResponse;
import com.thesystem.modules.xp.dto.policy.PolicyRequest;
import com.thesystem.modules.xp.dto.policy.PolicyResponse;
import com.thesystem.modules.xp.dto.statistics.LeaderboardEntry;
import com.thesystem.modules.xp.dto.statistics.LeaderboardResponse;
import com.thesystem.modules.xp.dto.statistics.StatisticsResponse;
import com.thesystem.modules.xp.dto.transaction.TransactionResponse;
import com.thesystem.modules.xp.dto.xpaccount.XpAccountResponse;
import com.thesystem.modules.xp.enums.AchievementCategory;
import com.thesystem.modules.xp.enums.PolicyType;
import com.thesystem.modules.xp.enums.RequirementType;
import com.thesystem.modules.xp.enums.TransactionType;
import com.thesystem.modules.xp.service.XpService;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class XpControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {
        static XpService xpService = Mockito.mock(XpService.class);

        @Bean
        XpService xpService() {
            return xpService;
        }
    }

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_USER")))
        );
    }

    @Test
    void shouldGetXpAccount() throws Exception {
        UUID accountId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        XpAccountResponse response = new XpAccountResponse(
                accountId, userId, 150, 3, 500, 50, 550, 50.0,
                Instant.now(), Instant.now()
        );

        Mockito.when(TestConfig.xpService.getAccount(any(UUID.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/xp/account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentXp").value(150));
    }

    @Test
    void shouldGetTransactions() throws Exception {
        UUID transactionId = UUID.randomUUID();
        TransactionResponse response = new TransactionResponse(
                transactionId, UUID.randomUUID(), TransactionType.TASK_COMPLETION, 50, 200,
                "TASK_ENGINE", UUID.randomUUID(), "TASK", null, 1.0, 50,
                "Task completed", Map.of(), Instant.now()
        );

        Mockito.when(TestConfig.xpService.listTransactions(any(UUID.class), any())).thenReturn(org.springframework.data.domain.Page.empty());

        mockMvc.perform(get("/api/v1/xp/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldGetTransactionById() throws Exception {
        UUID transactionId = UUID.randomUUID();
        TransactionResponse response = new TransactionResponse(
                transactionId, UUID.randomUUID(), TransactionType.TASK_COMPLETION, 50, 200,
                "TASK_ENGINE", UUID.randomUUID(), "TASK", null, 1.0, 50,
                "Task completed", Map.of(), Instant.now()
        );

        Mockito.when(TestConfig.xpService.getTransaction(any(UUID.class), any(UUID.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/xp/transactions/" + transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.amount").value(50));
    }

    @Test
    void shouldGetStatistics() throws Exception {
        StatisticsResponse response = new StatisticsResponse(
                100, 500, 2000, 5000, 5, 75.0, 20, 5, 3,
                Map.of("TASK_ENGINE", 3000L, "GOAL_ENGINE", 2000L),
                Map.of(TransactionType.TASK_COMPLETION, 3000L, TransactionType.GOAL_COMPLETION, 2000L),
                Map.of(AchievementCategory.TASK, 5L, AchievementCategory.GOAL, 3L),
                Instant.now()
        );

        Mockito.when(TestConfig.xpService.getStatistics()).thenReturn(response);

        mockMvc.perform(get("/api/v1/xp/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lifetimeXp").value(5000));
    }

    @Test
    void shouldGetLeaderboard() throws Exception {
        LeaderboardEntry entry = new LeaderboardEntry(UUID.randomUUID(), "user1", 5000, 10, 1);
        LeaderboardResponse response = new LeaderboardResponse(List.of(entry), 1, 1);

        Mockito.when(TestConfig.xpService.getLeaderboard(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/xp/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.entries[0].username").value("user1"));
    }

    @Test
    void shouldGetAllAchievements() throws Exception {
        AchievementResponse response = new AchievementResponse(
                UUID.randomUUID(), "FIRST_TASK", "First Task", "Complete your first task",
                AchievementCategory.TASK, null, RequirementType.COUNTER, Map.of("count", 1),
                100, false, false, 1, Instant.now()
        );

        Mockito.when(TestConfig.xpService.getAllAchievements()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/xp/achievements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("First Task"));
    }

    @Test
    void shouldGetAchievementById() throws Exception {
        UUID achievementId = UUID.randomUUID();
        AchievementResponse response = new AchievementResponse(
                achievementId, "FIRST_TASK", "First Task", "Complete your first task",
                AchievementCategory.TASK, null, RequirementType.COUNTER, Map.of("count", 1),
                100, false, false, 1, Instant.now()
        );

        Mockito.when(TestConfig.xpService.getAchievement(any(UUID.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/xp/achievements/" + achievementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("First Task"));
    }

    @Test
    void shouldGetUserAchievements() throws Exception {
        UserAchievementResponse response = new UserAchievementResponse(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "FIRST_TASK", "First Task",
                AchievementCategory.TASK, 1, 1, true, Instant.now(), Map.of(), Instant.now()
        );

        Mockito.when(TestConfig.xpService.getUserAchievements(any(UUID.class))).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/xp/achievements/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].achievementCode").value("FIRST_TASK"));
    }

    @Test
    void shouldCheckAchievements() throws Exception {
        AchievementResponse response = new AchievementResponse(
                UUID.randomUUID(), "STREAK_3", "3-Day Streak", "Complete tasks 3 days in a row",
                AchievementCategory.STREAK, null, RequirementType.STREAK, Map.of("days", 3),
                200, false, false, 2, Instant.now()
        );

        Mockito.when(TestConfig.xpService.checkAchievements(any(UUID.class))).thenReturn(List.of(response));

        mockMvc.perform(post("/api/v1/xp/achievements/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("3-Day Streak"));
    }

    @Test
    void shouldGetPolicies() throws Exception {
        PolicyResponse response = new PolicyResponse(
                UUID.randomUUID(), "TASK_XP", "Task XP Policy", "XP for completing tasks",
                PolicyType.TASK_COMPLETION, 50, 1.0, Map.of("difficulty", "normal"),
                true, 1, Instant.now(), Instant.now()
        );

        Mockito.when(TestConfig.xpService.getAllPolicies()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/xp/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Task XP Policy"));
    }

    @Test
    void shouldGetPolicyById() throws Exception {
        UUID policyId = UUID.randomUUID();
        PolicyResponse response = new PolicyResponse(
                policyId, "TASK_XP", "Task XP Policy", "XP for completing tasks",
                PolicyType.TASK_COMPLETION, 50, 1.0, Map.of("difficulty", "normal"),
                true, 1, Instant.now(), Instant.now()
        );

        Mockito.when(TestConfig.xpService.getPolicy(any(UUID.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/xp/policies/" + policyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Task XP Policy"));
    }

    @Test
    void shouldCreatePolicyAsAdmin() throws Exception {
        UUID policyId = UUID.randomUUID();
        PolicyRequest request = new PolicyRequest(
                "NEW_POLICY", "New Policy", "A new policy",
                PolicyType.BONUS, 100, 1.5, Map.of("condition", "value"), true, 5
        );
        PolicyResponse response = new PolicyResponse(
                policyId, "NEW_POLICY", "New Policy", "A new policy",
                PolicyType.BONUS, 100, 1.5, Map.of("condition", "value"),
                true, 5, Instant.now(), Instant.now()
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        Mockito.when(TestConfig.xpService.createPolicy(any(PolicyRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/xp/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("New Policy"));
    }

    @Test
    void shouldUpdatePolicyAsAdmin() throws Exception {
        UUID policyId = UUID.randomUUID();
        PolicyRequest request = new PolicyRequest(
                "UPDATED_POLICY", "Updated Policy", "An updated policy",
                PolicyType.BONUS, 200, 2.0, Map.of("condition", "updated"), true, 10
        );
        PolicyResponse response = new PolicyResponse(
                policyId, "UPDATED_POLICY", "Updated Policy", "An updated policy",
                PolicyType.BONUS, 200, 2.0, Map.of("condition", "updated"),
                true, 10, Instant.now(), Instant.now()
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        Mockito.when(TestConfig.xpService.updatePolicy(any(UUID.class), any(PolicyRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/xp/policies/" + policyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Updated Policy"));
    }

    @Test
    void shouldDeletePolicyAsAdmin() throws Exception {
        UUID policyId = UUID.randomUUID();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(UUID.randomUUID().toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );

        Mockito.doNothing().when(TestConfig.xpService).deletePolicy(any(UUID.class));

        mockMvc.perform(delete("/api/v1/xp/policies/" + policyId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void shouldForbidCreatePolicyAsNonAdmin() throws Exception {
        PolicyRequest request = new PolicyRequest(
                "NEW_POLICY", "New Policy", "A new policy",
                PolicyType.BONUS, 100, 1.5, Map.of("condition", "value"), true, 5
        );

        mockMvc.perform(post("/api/v1/xp/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldForbidUpdatePolicyAsNonAdmin() throws Exception {
        UUID policyId = UUID.randomUUID();
        PolicyRequest request = new PolicyRequest(
                "UPDATED_POLICY", "Updated Policy", "An updated policy",
                PolicyType.BONUS, 200, 2.0, Map.of("condition", "updated"), true, 10
        );

        mockMvc.perform(patch("/api/v1/xp/policies/" + policyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldForbidDeletePolicyAsNonAdmin() throws Exception {
        UUID policyId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/xp/policies/" + policyId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldGetUserAchievementReadOnly() throws Exception {
        UUID userAchievementId = UUID.randomUUID();
        UserAchievementResponse response = new UserAchievementResponse(
                userAchievementId, UUID.randomUUID(), UUID.randomUUID(), "FIRST_TASK", "First Task",
                AchievementCategory.TASK, 1, 1, true, Instant.now(), Map.of(), Instant.now()
        );

        Mockito.when(TestConfig.xpService.getUserAchievement(any(UUID.class), eq(userAchievementId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/xp/achievements/user/" + userAchievementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.achievementCode").value("FIRST_TASK"));
    }
}
