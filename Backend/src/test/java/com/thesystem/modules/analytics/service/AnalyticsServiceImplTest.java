package com.thesystem.modules.analytics.service;

import com.thesystem.modules.analytics.service.impl.AnalyticsServiceImpl;

import com.thesystem.modules.ai.provider.AiContextItem;
import com.thesystem.modules.ai.dto.AiInteractionResponse;
import com.thesystem.modules.ai.service.AiService;
import com.thesystem.modules.goal.dto.GoalResponse;
import com.thesystem.modules.goal.dto.GoalStatisticsResponse;
import com.thesystem.modules.goal.enums.GoalPriority;
import com.thesystem.modules.goal.enums.GoalStatus;
import com.thesystem.modules.goal.service.GoalService;
import com.thesystem.modules.memory.dto.MemoryFilterRequest;
import com.thesystem.modules.memory.dto.MemoryResponse;
import com.thesystem.modules.memory.enums.MemoryImportance;
import com.thesystem.modules.memory.enums.MemorySource;
import com.thesystem.modules.memory.enums.MemoryType;
import com.thesystem.modules.memory.service.MemoryService;
import com.thesystem.modules.task.dto.TaskFilterRequest;
import com.thesystem.modules.task.dto.TaskResponse;
import com.thesystem.modules.task.dto.TaskStatisticsResponse;
import com.thesystem.modules.task.dto.TimeEntryResponse;
import com.thesystem.modules.task.enums.TaskPriority;
import com.thesystem.modules.task.enums.TaskStatus;
import com.thesystem.modules.task.enums.TaskTimeEntryType;
import com.thesystem.modules.task.service.TaskService;
import com.thesystem.modules.user.service.UserTimezoneResolver;
import com.thesystem.modules.xp.dto.xpaccount.XpAccountResponse;
import com.thesystem.modules.xp.dto.streak.UserStreakResponse;
import com.thesystem.modules.xp.dto.transaction.TransactionResponse;
import com.thesystem.modules.xp.enums.TransactionType;
import com.thesystem.modules.xp.service.XpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplTest {

    @Mock
    private TaskService taskService;

    @Mock
    private GoalService goalService;

    @Mock
    private XpService xpService;

    @Mock
    private MemoryService memoryService;

    @Mock
    private AiService aiService;

    @Mock
    private UserTimezoneResolver userTimezoneResolver;

    private AnalyticsServiceImpl analyticsService;
    private UUID userId;
    private ZoneId zone;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsServiceImpl(
                taskService, goalService, xpService, memoryService, aiService, userTimezoneResolver
        );
        userId = UUID.randomUUID();
        zone = ZoneId.of("Asia/Kolkata");
        lenient().when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(zone);
    }

    // ========================
    // getOverview tests
    // ========================

    @Test
    void shouldComposeOverviewFromAllEngines() {
        UUID accountId = UUID.randomUUID();
        XpAccountResponse xpAccount = new XpAccountResponse(
                accountId, userId, 100, 5, 200, 0, 500, 0.5,
                Instant.now(), Instant.now()
        );
        UserStreakResponse streak = new UserStreakResponse(7, 14, LocalDate.now(), LocalDate.now());
        TaskStatisticsResponse taskStats = new TaskStatisticsResponse(
                0.5, 10L, 5L, 3L, 2L, 1L, 0L, 0.0, 0L, 0L, 0L,
                Map.of(), Map.of(), Map.of()
        );
        GoalStatisticsResponse goalStats = new GoalStatisticsResponse(
                4L, 2L, 1L, 1L, 0L, 50.0,
                Map.of(), Map.of()
        );
        var memoryUsage = new com.thesystem.modules.analytics.dto.MemoryUsageAnalyticsResponse(
                3L, Map.of(), Map.of(), Map.of(), List.of()
        );
        var aiUsage = new com.thesystem.modules.analytics.dto.AiUsageAnalyticsResponse(
                5L, 100L, 60L, 40L, 20.0, Map.of(), Map.of(), List.of()
        );

        when(xpService.getAccount(userId)).thenReturn(xpAccount);
        when(xpService.getUserStreak(userId)).thenReturn(streak);
        when(taskService.getStatistics(userId)).thenReturn(taskStats);
        when(goalService.getStatistics(userId)).thenReturn(goalStats);
        when(aiService.listInteractions(userId)).thenReturn(List.of());
        when(memoryService.listMemories(any(), any())).thenReturn(List.of());

        var overview = analyticsService.getOverview(userId);

        assertThat(overview.xpAccount()).isEqualTo(xpAccount);
        assertThat(overview.streak()).isEqualTo(streak);
        assertThat(overview.taskStatistics()).isEqualTo(taskStats);
        assertThat(overview.goalStatistics()).isEqualTo(goalStats);
    }

    @Test
    void shouldReturnZeroedStreakWhenExceptionOccurs() {
        XpAccountResponse xpAccount = new XpAccountResponse(
                UUID.randomUUID(), userId, 0, 1, 0, 0, 0, 0.0,
                Instant.now(), Instant.now()
        );
        TaskStatisticsResponse taskStats = new TaskStatisticsResponse(
                0.0, 0L, 0L, 0L, 0L, 0L, 0L, 0.0, 0L, 0L, 0L,
                Map.of(), Map.of(), Map.of()
        );
        GoalStatisticsResponse goalStats = new GoalStatisticsResponse(
                0L, 0L, 0L, 0L, 0L, 0.0,
                Map.of(), Map.of()
        );

        when(xpService.getAccount(userId)).thenReturn(xpAccount);
        when(xpService.getUserStreak(userId)).thenThrow(new RuntimeException("streak error"));
        when(taskService.getStatistics(userId)).thenReturn(taskStats);
        when(goalService.getStatistics(userId)).thenReturn(goalStats);
        when(aiService.listInteractions(userId)).thenReturn(List.of());
        when(memoryService.listMemories(any(), any())).thenReturn(List.of());

        var overview = analyticsService.getOverview(userId);

        assertThat(overview.streak().currentStreak()).isZero();
        assertThat(overview.streak().longestStreak()).isZero();
    }

    // ========================
    // getTrends tests
    // ========================

    @Test
    void shouldRejectDaysZero() {
        assertThatThrownBy(() -> analyticsService.getTrends(userId, 0))
                .isInstanceOf(com.thesystem.common.exception.BusinessException.class);
    }

    @Test
    void shouldRejectDaysNegative() {
        assertThatThrownBy(() -> analyticsService.getTrends(userId, -1))
                .isInstanceOf(com.thesystem.common.exception.BusinessException.class);
    }

    @Test
    void shouldRejectDaysAboveMax() {
        assertThatThrownBy(() -> analyticsService.getTrends(userId, 91))
                .isInstanceOf(com.thesystem.common.exception.BusinessException.class);
    }

    @Test
    void shouldAcceptDaysOne() {
        LocalDate today = LocalDate.now(zone);

        lenient().when(xpService.getTransactionHistory(any(), any())).thenReturn(List.of());
        lenient().when(taskService.listTasks(any(), any())).thenReturn(List.of());
        lenient().when(goalService.getGoals(any(), any())).thenReturn(List.of());
        lenient().when(taskService.listTimeEntriesForPeriod(any(), any(), any())).thenReturn(List.of());
        lenient().when(xpService.getActivityTrend(any(), any(), any())).thenReturn(List.of());

        var trends = analyticsService.getTrends(userId, 1);

        assertThat(trends.days()).isEqualTo(1);
        assertThat(trends.from()).isEqualTo(today);
        assertThat(trends.to()).isEqualTo(today);
        assertThat(trends.xpEarned()).hasSize(1);
        assertThat(trends.tasksCompleted()).hasSize(1);
        assertThat(trends.goalsCompleted()).hasSize(1);
        assertThat(trends.focusMinutes()).hasSize(1);
        assertThat(trends.activityDays()).hasSize(1);
    }

    @Test
    void shouldAcceptDaysNinety() {
        lenient().when(xpService.getTransactionHistory(any(), any())).thenReturn(List.of());
        lenient().when(taskService.listTasks(any(), any())).thenReturn(List.of());
        lenient().when(goalService.getGoals(any(), any())).thenReturn(List.of());
        lenient().when(taskService.listTimeEntriesForPeriod(any(), any(), any())).thenReturn(List.of());
        lenient().when(xpService.getActivityTrend(any(), any(), any())).thenReturn(List.of());

        var trends = analyticsService.getTrends(userId, 90);

        assertThat(trends.days()).isEqualTo(90);
        assertThat(trends.xpEarned()).hasSize(90);
    }

    @Test
    void shouldDefaultToFourteenDays() {
        lenient().when(xpService.getTransactionHistory(any(), any())).thenReturn(List.of());
        lenient().when(taskService.listTasks(any(), any())).thenReturn(List.of());
        lenient().when(goalService.getGoals(any(), any())).thenReturn(List.of());
        lenient().when(taskService.listTimeEntriesForPeriod(any(), any(), any())).thenReturn(List.of());
        lenient().when(xpService.getActivityTrend(any(), any(), any())).thenReturn(List.of());

        var trends = analyticsService.getTrends(userId, 14);

        assertThat(trends.days()).isEqualTo(14);
        assertThat(trends.xpEarned()).hasSize(14);
    }

    @Test
    void shouldBucketXpByDayPositiveAmountOnly() {
        LocalDate today = LocalDate.now(zone);
        LocalDate yesterday = today.minusDays(1);
        UUID txnId = UUID.randomUUID();

        TransactionResponse positiveTxn = new TransactionResponse(
                txnId, userId, TransactionType.TASK_COMPLETION, 50, 100, "earned", null, "task", null, 1.0, 50, null, Map.of(),
                yesterday.atStartOfDay(zone).toInstant()
        );
        TransactionResponse negativeTxn = new TransactionResponse(
                UUID.randomUUID(), userId, TransactionType.TASK_COMPLETION, -10, 90, "spent", null, "reward", null, 1.0, -10, null, Map.of(),
                yesterday.atStartOfDay(zone).toInstant()
        );

        when(xpService.getTransactionHistory(any(), any())).thenReturn(List.of(positiveTxn, negativeTxn));
        lenient().when(taskService.listTasks(any(), any())).thenReturn(List.of());
        lenient().when(goalService.getGoals(any(), any())).thenReturn(List.of());
        lenient().when(taskService.listTimeEntriesForPeriod(any(), any(), any())).thenReturn(List.of());
        lenient().when(xpService.getActivityTrend(any(), any(), any())).thenReturn(List.of());

        var trends = analyticsService.getTrends(userId, 14);

        var yesterdayPoint = trends.xpEarned().stream()
                .filter(p -> p.date().equals(yesterday))
                .findFirst()
                .orElseThrow();
        assertThat(yesterdayPoint.value()).isEqualTo(50L);
    }

    @Test
    void shouldBucketTasksByCompletedDate() {
        LocalDate today = LocalDate.now(zone);
        UUID taskId = UUID.randomUUID();

        TaskResponse completedTask = new TaskResponse(
                taskId, userId, null, null, "Task", "desc",
                TaskStatus.COMPLETED, null, null, null, null, null, null,
                today.atStartOfDay(zone).toInstant(), null,
                today.atStartOfDay(zone).toInstant(), null,
                null, null, List.of(), List.of(), null, Map.of(), Map.of(), Map.of(),
                null, Instant.now(), Instant.now(), null, null, null
        );

        when(xpService.getTransactionHistory(any(), any())).thenReturn(List.of());
        when(taskService.listTasks(any(), any())).thenReturn(List.of(completedTask));
        lenient().when(goalService.getGoals(any(), any())).thenReturn(List.of());
        lenient().when(taskService.listTimeEntriesForPeriod(any(), any(), any())).thenReturn(List.of());
        lenient().when(xpService.getActivityTrend(any(), any(), any())).thenReturn(List.of());

        var trends = analyticsService.getTrends(userId, 14);

        var todayPoint = trends.tasksCompleted().stream()
                .filter(p -> p.date().equals(today))
                .findFirst()
                .orElseThrow();
        assertThat(todayPoint.value()).isEqualTo(1L);
    }

    @Test
    void shouldSumFocusMinutesByDay() {
        LocalDate today = LocalDate.now(zone);
        UUID entryId = UUID.randomUUID();

        TimeEntryResponse entry = new TimeEntryResponse(
                entryId, null, userId,
                today.atStartOfDay(zone).toInstant(), null,
                30, TaskTimeEntryType.MANUAL, null, Instant.now(), null, null
        );

        when(xpService.getTransactionHistory(any(), any())).thenReturn(List.of());
        lenient().when(taskService.listTasks(any(), any())).thenReturn(List.of());
        lenient().when(goalService.getGoals(any(), any())).thenReturn(List.of());
        when(taskService.listTimeEntriesForPeriod(any(), any(), any())).thenReturn(List.of(entry));
        lenient().when(xpService.getActivityTrend(any(), any(), any())).thenReturn(List.of());

        var trends = analyticsService.getTrends(userId, 14);

        var todayPoint = trends.focusMinutes().stream()
                .filter(p -> p.date().equals(today))
                .findFirst()
                .orElseThrow();
        assertThat(todayPoint.value()).isEqualTo(30L);
    }

    @Test
    void shouldAggregateXpByDayOfWeek() {
        LocalDate monday = LocalDate.now(zone);
        while (monday.getDayOfWeek() != java.time.DayOfWeek.MONDAY) {
            monday = monday.minusDays(1);
        }

        TransactionResponse mondayTxn = new TransactionResponse(
                UUID.randomUUID(), userId, TransactionType.TASK_COMPLETION, 50, 100, "earned", null, "task", null, 1.0, 50, null, Map.of(),
                monday.atStartOfDay(zone).toInstant()
        );

        when(xpService.getTransactionHistory(any(), any())).thenReturn(List.of(mondayTxn));
        lenient().when(taskService.listTasks(any(), any())).thenReturn(List.of());
        lenient().when(goalService.getGoals(any(), any())).thenReturn(List.of());
        lenient().when(taskService.listTimeEntriesForPeriod(any(), any(), any())).thenReturn(List.of());
        lenient().when(xpService.getActivityTrend(any(), any(), any())).thenReturn(List.of());

        var trends = analyticsService.getTrends(userId, 14);

        assertThat(trends.xpByDayOfWeek()).containsKey("MONDAY");
        assertThat(trends.xpByDayOfWeek().get("MONDAY")).isEqualTo(50L);
    }

    @Test
    void shouldZeroFillEmptyWindow() {
        lenient().when(xpService.getTransactionHistory(any(), any())).thenReturn(List.of());
        lenient().when(taskService.listTasks(any(), any())).thenReturn(List.of());
        lenient().when(goalService.getGoals(any(), any())).thenReturn(List.of());
        lenient().when(taskService.listTimeEntriesForPeriod(any(), any(), any())).thenReturn(List.of());
        lenient().when(xpService.getActivityTrend(any(), any(), any())).thenReturn(List.of());

        var trends = analyticsService.getTrends(userId, 14);

        assertThat(trends.xpEarned()).allSatisfy(p -> assertThat(p.value()).isZero());
        assertThat(trends.tasksCompleted()).allSatisfy(p -> assertThat(p.value()).isZero());
        assertThat(trends.goalsCompleted()).allSatisfy(p -> assertThat(p.value()).isZero());
        assertThat(trends.focusMinutes()).allSatisfy(p -> assertThat(p.value()).isZero());
        assertThat(trends.activityDays()).allSatisfy(p -> assertThat(p.value()).isZero());
    }

    // ========================
    // getAiUsage tests
    // ========================

    @Test
    void shouldAggregateAiUsage() {
        UUID interactionId = UUID.randomUUID();
        Instant now = Instant.now();
        List<AiContextItem> context = List.of();
        AiInteractionResponse interaction = new AiInteractionResponse(
                interactionId, userId, "prompt", "response",
                "openai", "gpt-4", context,
                Integer.valueOf(100), Integer.valueOf(50), Integer.valueOf(150), "stop",
                now, now, null, null, null
        );

        when(aiService.listInteractions(userId)).thenReturn(List.of(interaction));

        var aiUsage = analyticsService.getAiUsage(userId);

        assertThat(aiUsage.totalInteractions()).isEqualTo(1);
        assertThat(aiUsage.totalTokens()).isEqualTo(150);
        assertThat(aiUsage.promptTokens()).isEqualTo(100);
        assertThat(aiUsage.completionTokens()).isEqualTo(50);
        assertThat(aiUsage.avgTokensPerInteraction()).isEqualTo(150.0);
        assertThat(aiUsage.byProvider()).containsEntry("openai", 1L);
        assertThat(aiUsage.byModel()).containsEntry("gpt-4", 1L);
    }

    @Test
    void shouldHandleNullModelInAiUsage() {
        Instant now = Instant.now();
        List<AiContextItem> context = List.of();
        AiInteractionResponse interaction = new AiInteractionResponse(
                UUID.randomUUID(), userId, "prompt", "response",
                "openai", null, context,
                Integer.valueOf(100), Integer.valueOf(50), Integer.valueOf(150), "stop",
                now, now, null, null, null
        );

        when(aiService.listInteractions(userId)).thenReturn(List.of(interaction));

        var aiUsage = analyticsService.getAiUsage(userId);

        assertThat(aiUsage.byModel()).containsEntry("unknown", 1L);
    }

    @Test
    void shouldReturnEmptyAiUsageWhenNoInteractions() {
        when(aiService.listInteractions(userId)).thenReturn(List.of());

        var aiUsage = analyticsService.getAiUsage(userId);

        assertThat(aiUsage.totalInteractions()).isZero();
        assertThat(aiUsage.totalTokens()).isZero();
        assertThat(aiUsage.avgTokensPerInteraction()).isZero();
        assertThat(aiUsage.byProvider()).isEmpty();
        assertThat(aiUsage.byModel()).isEmpty();
        assertThat(aiUsage.perDay()).isEmpty();
    }

    // ========================
    // getMemoryUsage tests
    // ========================

    @Test
    void shouldAggregateMemoryUsage() {
        UUID memoryId = UUID.randomUUID();
        Instant now = Instant.now();
        MemoryResponse memory = new MemoryResponse(
                memoryId, userId, "Test", "Content",
                MemoryType.NOTE, MemoryImportance.NORMAL, MemorySource.MANUAL,
                null, List.of(), Map.of(), now, now, null, null, null
        );

        when(memoryService.listMemories(any(), any())).thenReturn(List.of(memory));

        var memoryUsage = analyticsService.getMemoryUsage(userId);

        assertThat(memoryUsage.totalMemories()).isEqualTo(1);
        assertThat(memoryUsage.byType()).containsEntry("NOTE", 1L);
        assertThat(memoryUsage.byImportance()).containsEntry("NORMAL", 1L);
        assertThat(memoryUsage.bySource()).containsEntry("MANUAL", 1L);
    }

    @Test
    void shouldReturnEmptyMemoryUsageWhenNoMemories() {
        when(memoryService.listMemories(any(), any())).thenReturn(List.of());

        var memoryUsage = analyticsService.getMemoryUsage(userId);

        assertThat(memoryUsage.totalMemories()).isZero();
        assertThat(memoryUsage.byType()).isEmpty();
        assertThat(memoryUsage.byImportance()).isEmpty();
        assertThat(memoryUsage.bySource()).isEmpty();
        assertThat(memoryUsage.perMonth()).isEmpty();
    }
}
