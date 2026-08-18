package com.thesystem.modules.analytics.service;

import com.thesystem.modules.analytics.dto.*;
import java.util.UUID;

public interface AnalyticsService {

    AnalyticsOverviewResponse getOverview(UUID userId);

    AnalyticsTrendsResponse getTrends(UUID userId, int days);

    AiUsageAnalyticsResponse getAiUsage(UUID userId);

    MemoryUsageAnalyticsResponse getMemoryUsage(UUID userId);
}