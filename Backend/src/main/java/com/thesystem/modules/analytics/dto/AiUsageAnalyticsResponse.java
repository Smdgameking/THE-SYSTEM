package com.thesystem.modules.analytics.dto;

import java.util.List;
import java.util.Map;

public record AiUsageAnalyticsResponse(
        long totalInteractions,
        long totalTokens,
        long promptTokens,
        long completionTokens,
        double avgTokensPerInteraction,
        Map<String, Long> byProvider,
        Map<String, Long> byModel,
        List<DailyPoint> perDay
) {
}
