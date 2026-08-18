package com.thesystem.modules.analytics.dto;

import com.thesystem.modules.xp.dto.xpaccount.XpAccountResponse;
import com.thesystem.modules.xp.dto.streak.UserStreakResponse;
import com.thesystem.modules.task.dto.TaskStatisticsResponse;
import com.thesystem.modules.goal.dto.GoalStatisticsResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AnalyticsOverviewResponse(
        XpAccountResponse xpAccount,
        UserStreakResponse streak,
        TaskStatisticsResponse taskStatistics,
        GoalStatisticsResponse goalStatistics,
        MemoryUsageAnalyticsResponse memoryUsage,
        AiUsageAnalyticsResponse aiUsage
) {
}