package com.thesystem.modules.analytics.controller;

import com.thesystem.common.response.ApiResponse;
import com.thesystem.modules.analytics.dto.*;
import com.thesystem.modules.analytics.service.AnalyticsService;
import com.thesystem.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get cross-domain per-user overview snapshot")
    public ResponseEntity<ApiResponse<AnalyticsOverviewResponse>> overview() {
        UUID userId = SecurityUtils.getCurrentUserId();
        AnalyticsOverviewResponse data = analyticsService.getOverview(userId);
        return ResponseEntity.ok(ApiResponse.ok(data, "Overview retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/trends")
    @Operation(summary = "Get time-series trends over a bounded look-back window")
    public ResponseEntity<ApiResponse<AnalyticsTrendsResponse>> trends(
            @RequestParam(value = "days", defaultValue = "14") int days) {
        UUID userId = SecurityUtils.getCurrentUserId();
        AnalyticsTrendsResponse data = analyticsService.getTrends(userId, days);
        return ResponseEntity.ok(ApiResponse.ok(data, "Trends retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/ai-usage")
    @Operation(summary = "Get AI interaction usage reporting")
    public ResponseEntity<ApiResponse<AiUsageAnalyticsResponse>> aiUsage() {
        UUID userId = SecurityUtils.getCurrentUserId();
        AiUsageAnalyticsResponse data = analyticsService.getAiUsage(userId);
        return ResponseEntity.ok(ApiResponse.ok(data, "AI usage retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/memories")
    @Operation(summary = "Get memory usage reporting")
    public ResponseEntity<ApiResponse<MemoryUsageAnalyticsResponse>> memories() {
        UUID userId = SecurityUtils.getCurrentUserId();
        MemoryUsageAnalyticsResponse data = analyticsService.getMemoryUsage(userId);
        return ResponseEntity.ok(ApiResponse.ok(data, "Memory usage retrieved successfully", UUID.randomUUID().toString()));
    }
}