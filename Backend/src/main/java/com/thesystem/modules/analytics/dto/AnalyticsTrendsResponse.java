package com.thesystem.modules.analytics.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record AnalyticsTrendsResponse(
        int days,
        LocalDate from,
        LocalDate to,
        List<DailyPoint> xpEarned,
        List<DailyPoint> tasksCompleted,
        List<DailyPoint> goalsCompleted,
        List<DailyPoint> focusMinutes,
        List<DailyPoint> activityDays,
        Map<String, Long> xpByDayOfWeek
) {
}
