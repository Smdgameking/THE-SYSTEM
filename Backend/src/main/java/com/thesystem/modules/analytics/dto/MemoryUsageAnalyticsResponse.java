package com.thesystem.modules.analytics.dto;

import java.util.List;
import java.util.Map;

public record MemoryUsageAnalyticsResponse(
        long totalMemories,
        Map<String, Long> byType,
        Map<String, Long> byImportance,
        Map<String, Long> bySource,
        List<MonthlyPoint> perMonth
) {
}
