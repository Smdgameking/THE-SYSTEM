package com.thesystem.modules.analytics.service.impl;

import com.thesystem.common.constants.ErrorCodes;
import com.thesystem.common.exception.BusinessException;
import com.thesystem.modules.analytics.dto.*;
import com.thesystem.modules.analytics.service.AnalyticsService;
import com.thesystem.modules.ai.service.AiService;
import com.thesystem.modules.ai.dto.AiInteractionResponse;
import com.thesystem.modules.goal.service.GoalService;
import com.thesystem.modules.goal.dto.GoalResponse;
import com.thesystem.modules.memory.service.MemoryService;
import com.thesystem.modules.memory.dto.MemoryResponse;
import com.thesystem.modules.memory.dto.MemoryFilterRequest;
import com.thesystem.modules.task.service.TaskService;
import com.thesystem.modules.task.dto.TaskResponse;
import com.thesystem.modules.task.dto.TimeEntryResponse;
import com.thesystem.modules.task.dto.TaskFilterRequest;
import com.thesystem.modules.task.enums.TaskStatus;
import com.thesystem.modules.xp.service.XpService;
import com.thesystem.modules.xp.service.XpService.ActivityDay;
import com.thesystem.modules.xp.dto.transaction.TransactionResponse;
import com.thesystem.modules.xp.dto.transaction.TransactionHistoryFilter;
import com.thesystem.modules.xp.dto.xpaccount.XpAccountResponse;
import com.thesystem.modules.xp.dto.streak.UserStreakResponse;
import com.thesystem.modules.xp.exception.XpAccountNotFoundException;
import com.thesystem.modules.user.service.UserTimezoneResolver;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final int MAX_TREND_DAYS = 90;
    private static final int DEFAULT_TREND_DAYS = 14;

    private final TaskService taskService;
    private final GoalService goalService;
    private final XpService xpService;
    private final MemoryService memoryService;
    private final AiService aiService;
    private final UserTimezoneResolver userTimezoneResolver;

    private static final Logger log = LoggerFactory.getLogger(AnalyticsServiceImpl.class);

    public AnalyticsServiceImpl(TaskService taskService,
                                GoalService goalService,
                                XpService xpService,
                                MemoryService memoryService,
                                AiService aiService,
                                UserTimezoneResolver userTimezoneResolver) {
        this.taskService = taskService;
        this.goalService = goalService;
        this.xpService = xpService;
        this.memoryService = memoryService;
        this.aiService = aiService;
        this.userTimezoneResolver = userTimezoneResolver;
    }

    @Override
    public AnalyticsOverviewResponse getOverview(UUID userId) {
        XpAccountResponse xpAccount;
        try {
            xpAccount = xpService.getAccount(userId);
        } catch (XpAccountNotFoundException e) {
            xpAccount = new XpAccountResponse(
                    UUID.randomUUID(), userId, 0, 1, 0, 0, 0, 0.0,
                    java.time.Instant.now(), java.time.Instant.now()
            );
        }
        UserStreakResponse streak;
        try {
            streak = xpService.getUserStreak(userId);
        } catch (Exception e) {
            streak = new com.thesystem.modules.xp.dto.streak.UserStreakResponse(0, 0, null, null);
        }
        return new AnalyticsOverviewResponse(
                xpAccount,
                streak,
                taskService.getStatistics(userId),
                goalService.getStatistics(userId),
                getMemoryUsage(userId),
                getAiUsage(userId)
        );
    }

    @Override
    public AnalyticsTrendsResponse getTrends(UUID userId, int days) {
        if (days < 1 || days > MAX_TREND_DAYS) {
            throw new BusinessException(ErrorCodes.BAD_REQUEST,
                    "days must be between 1 and " + MAX_TREND_DAYS);
        }

        ZoneId zone = userTimezoneResolver.resolveUserZoneId(userId);
        LocalDate today = LocalDate.now(zone);
        LocalDate from = today.minusDays(days - 1);
        LocalDate toExclusive = today.plusDays(1);

        Instant fromInstant = from.atStartOfDay(zone).toInstant();
        Instant toExclusiveInstant = toExclusive.atStartOfDay(zone).toInstant();

        // ---------- 1. XP earned per day ----------
        List<TransactionResponse> xpTxns = xpService.getTransactionHistory(userId,
                new com.thesystem.modules.xp.dto.transaction.TransactionHistoryFilter(
                        null, null, fromInstant, toExclusiveInstant));
        Map<LocalDate, Long> xpByDate = new TreeMap<>();
        for (TransactionResponse txn : xpTxns) {
            if (txn.amount() > 0) {
                LocalDate day = txn.createdAt().atZone(zone).toLocalDate();
                if (!day.isBefore(from) && !day.isAfter(today)) {
                    xpByDate.merge(day, txn.amount().longValue(), Long::sum);
                }
            }
        }
        List<DailyPoint> xpEarned = zeroFillSeries(xpByDate, from, today);

        // ---------- 2. Tasks completed per day ----------
        List<TaskResponse> allTasks = taskService.listTasks(userId, new com.thesystem.modules.task.dto.TaskFilterRequest(
                null, null, null, null, null, null, null, null, null, null, null, null, 0, 0));
        Map<LocalDate, Long> tasksByDate = allTasks.stream()
                .filter(t -> t.status() == com.thesystem.modules.task.enums.TaskStatus.COMPLETED)
                .filter(t -> t.completedDate() != null)
                .filter(t -> !t.completedDate().isBefore(from.atStartOfDay(zone).toInstant())
                        && !t.completedDate().isAfter(toExclusiveInstant))
                .collect(Collectors.groupingBy(
                        t -> t.completedDate().atZone(zone).toLocalDate(),
                        Collectors.counting()));
        List<DailyPoint> tasksCompleted = zeroFillSeries(tasksByDate, from, today);

        // ---------- 3. Goals completed per day ----------
        List<GoalResponse> allGoals = goalService.getGoals(userId, new GoalService.GoalFilter(
                null, null, null, null, 0, Integer.MAX_VALUE));
        Map<LocalDate, Long> goalsByDate = allGoals.stream()
                .filter(g -> g.status() == com.thesystem.modules.goal.enums.GoalStatus.COMPLETED)
                .filter(g -> g.completedDate() != null)
                .filter(g -> !g.completedDate().isBefore(from.atStartOfDay(zone).toInstant())
                        && !g.completedDate().isAfter(toExclusiveInstant))
                .collect(Collectors.groupingBy(
                        g -> g.completedDate().atZone(zone).toLocalDate(),
                        Collectors.counting()));
        List<DailyPoint> goalsCompleted = zeroFillSeries(goalsByDate, from, today);

        // ---------- 4. Focus minutes per day ----------
        List<TimeEntryResponse> timeEntries = taskService.listTimeEntriesForPeriod(userId, fromInstant, toExclusiveInstant);
        Map<LocalDate, Long> focusByDate = timeEntries.stream()
                .filter(te -> te.durationMinutes() != null && te.durationMinutes() > 0)
                .collect(Collectors.groupingBy(
                        te -> te.startTime().atZone(zone).toLocalDate(),
                        Collectors.summingLong(TimeEntryResponse::durationMinutes)));
        List<DailyPoint> focusMinutes = zeroFillSeries(focusByDate, from, today);

        // ---------- 5. Activity days (streak history) ----------
        List<XpService.ActivityDay> activity = xpService.getActivityTrend(userId, from, toExclusive);
        Map<LocalDate, Long> activityByDate = activity.stream()
                .collect(Collectors.toMap(XpService.ActivityDay::date, XpService.ActivityDay::count));
        List<DailyPoint> activityDays = zeroFillSeries(activityByDate, from, today);

        // ---------- 6. XP by day of week ----------
        Map<String, Long> xpByDayOfWeek = xpTxns.stream()
                .filter(txn -> txn.amount() > 0)
                .collect(Collectors.groupingBy(
                        txn -> txn.createdAt().atZone(zone).getDayOfWeek().name(),
                        Collectors.summingLong(TransactionResponse::amount)));

        return new AnalyticsTrendsResponse(days, from, today, xpEarned, tasksCompleted,
                goalsCompleted, focusMinutes, activityDays, xpByDayOfWeek);
    }

    @Override
    public AiUsageAnalyticsResponse getAiUsage(UUID userId) {
        List<AiInteractionResponse> interactions = aiService.listInteractions(userId);
        long totalInteractions = interactions.size();
        long totalTokens = 0L;
        long promptTokens = 0L;
        long completionTokens = 0L;

        for (AiInteractionResponse interaction : interactions) {
            if (interaction.totalTokens() != null) totalTokens += interaction.totalTokens();
            if (interaction.promptTokens() != null) promptTokens += interaction.promptTokens();
            if (interaction.completionTokens() != null) completionTokens += interaction.completionTokens();
        }
        double avgTokensPerInteraction = totalInteractions > 0
                ? (double) totalTokens / totalInteractions
                : 0.0;

        Map<String, Long> byProvider = interactions.stream()
                .collect(Collectors.groupingBy(AiInteractionResponse::provider, Collectors.counting()));

        Map<String, Long> byModel = interactions.stream()
                .collect(Collectors.groupingBy(
                        interaction -> interaction.model() != null ? interaction.model() : "unknown",
                        Collectors.counting()));

        // per-day series
        ZoneId zone = userTimezoneResolver.resolveUserZoneId(userId);
        List<DailyPoint> perDay = new ArrayList<>();
        if (!interactions.isEmpty()) {
            Instant earliest = interactions.stream()
                    .map(AiInteractionResponse::createdAt)
                    .min(Comparator.naturalOrder()).orElse(null);
            Instant latest = interactions.stream()
                    .map(AiInteractionResponse::createdAt)
                    .max(Comparator.naturalOrder()).orElse(null);
            if (earliest != null && latest != null) {
                LocalDate start = earliest.atZone(zone).toLocalDate();
                LocalDate end = latest.atZone(zone).toLocalDate();
                Map<LocalDate, Long> dailyCounts = interactions.stream()
                        .collect(Collectors.groupingBy(
                                interaction -> interaction.createdAt().atZone(zone).toLocalDate(),
                                Collectors.counting()));
                perDay = zeroFillSeriesPerMonth(dailyCounts, start, end);
            }
        }

        return new AiUsageAnalyticsResponse(
                totalInteractions, totalTokens, promptTokens, completionTokens,
                avgTokensPerInteraction, byProvider, byModel, perDay);
    }

    @Override
    public MemoryUsageAnalyticsResponse getMemoryUsage(UUID userId) {
        ZoneId zone = userTimezoneResolver.resolveUserZoneId(userId);
        MemoryFilterRequest filter = new MemoryFilterRequest(
                null, null, null, null, null, null, null, null);
        List<MemoryResponse> memories = memoryService.listMemories(userId, filter);

        Map<String, Long> byType = memories.stream()
                .collect(Collectors.groupingBy(m -> m.type().name(), Collectors.counting()));

        Map<String, Long> byImportance = memories.stream()
                .collect(Collectors.groupingBy(m -> m.importance().name(), Collectors.counting()));

        Map<String, Long> bySource = memories.stream()
                .collect(Collectors.groupingBy(m -> m.source().name(), Collectors.counting()));

        // per-month series (ordered ascending, returning existing months)
        Map<String, Long> perMonthMap = memories.stream()
                .collect(Collectors.groupingBy(
                        m -> m.createdAt().atZone(zone).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")),
                        Collectors.counting()));

        List<MonthlyPoint> perMonth = new ArrayList<>();
        if (!perMonthMap.isEmpty()) {
            List<String> sortedMonths = new ArrayList<>(perMonthMap.keySet());
            Collections.sort(sortedMonths);
            for (String month : sortedMonths) {
                perMonth.add(new MonthlyPoint(month, perMonthMap.get(month)));
            }
        }

        return new MemoryUsageAnalyticsResponse(
                (long) memories.size(), byType, byImportance, bySource, perMonth);
    }

    // ---------- Helper: zero-fill a series map over a date range ----------
    private List<DailyPoint> zeroFillSeries(Map<LocalDate, Long> data, LocalDate from, LocalDate to) {
        List<DailyPoint> result = new ArrayList<>();
        LocalDate d = from;
        while (!d.isAfter(to)) {
            result.add(new DailyPoint(d, data.getOrDefault(d, 0L)));
            d = d.plusDays(1);
        }
        return result;
    }

    // Overloaded zero-fill for ai per-day that takes start/end LocalDate and a map
    private List<DailyPoint> zeroFillSeriesPerMonth(Map<LocalDate, Long> data, LocalDate start, LocalDate end) {
        List<DailyPoint> result = new ArrayList<>();
        LocalDate d = start;
        while (!d.isAfter(end)) {
            result.add(new DailyPoint(d, data.getOrDefault(d, 0L)));
            d = d.plusDays(1);
        }
        return result;
    }
}