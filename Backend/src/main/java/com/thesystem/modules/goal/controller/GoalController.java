package com.thesystem.modules.goal.controller;

import com.thesystem.common.response.ApiResponse;
import com.thesystem.modules.goal.dto.CreateGoalRequest;
import com.thesystem.modules.goal.dto.CreateMilestoneRequest;
import com.thesystem.modules.goal.dto.GoalDetailResponse;
import com.thesystem.modules.goal.dto.GoalResponse;
import com.thesystem.modules.goal.dto.GoalStatisticsResponse;
import com.thesystem.modules.goal.dto.MilestoneResponse;
import com.thesystem.modules.goal.dto.UpdateGoalRequest;
import com.thesystem.modules.goal.dto.UpdateMilestoneRequest;
import com.thesystem.modules.goal.enums.GoalPriority;
import com.thesystem.modules.goal.enums.GoalStatus;
import com.thesystem.modules.goal.service.GoalService;
import com.thesystem.security.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/goals")
@Tag(name = "Goal")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    @Operation(summary = "Create a new goal")
    public ResponseEntity<ApiResponse<GoalResponse>> createGoal(@Valid @RequestBody CreateGoalRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GoalResponse response = goalService.createGoal(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Goal created successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get goal details")
    public ResponseEntity<ApiResponse<GoalDetailResponse>> getGoal(@PathVariable @Parameter(description = "Goal ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GoalDetailResponse response = goalService.getGoal(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Goal retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping
    @Operation(summary = "List user goals")
    public ResponseEntity<ApiResponse<List<GoalResponse>>> getGoals(
            @Parameter(description = "Filter by status") @RequestParam(required = false) String status,
            @Parameter(description = "Filter by priority") @RequestParam(required = false) String priority,
            @Parameter(description = "Filter by category") @RequestParam(required = false) String category,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GoalService.GoalFilter filter = new GoalService.GoalFilter(
                status != null ? GoalStatus.valueOf(status) : null,
                priority != null ? GoalPriority.valueOf(priority) : null,
                category,
                null,
                page,
                size
        );
        List<GoalResponse> response = goalService.getGoals(userId, filter);
        return ResponseEntity.ok(ApiResponse.ok(response, "Goals retrieved successfully", UUID.randomUUID().toString()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update goal")
    public ResponseEntity<ApiResponse<GoalResponse>> updateGoal(@PathVariable @Parameter(description = "Goal ID") UUID id, @Valid @RequestBody UpdateGoalRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GoalResponse response = goalService.updateGoal(userId, id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Goal updated successfully", UUID.randomUUID().toString()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete goal")
    public ResponseEntity<ApiResponse<Void>> deleteGoal(@PathVariable @Parameter(description = "Goal ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        goalService.deleteGoal(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Goal deleted successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start goal")
    public ResponseEntity<ApiResponse<GoalResponse>> startGoal(@PathVariable @Parameter(description = "Goal ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GoalResponse response = goalService.startGoal(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Goal started successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/pause")
    @Operation(summary = "Pause goal")
    public ResponseEntity<ApiResponse<GoalResponse>> pauseGoal(@PathVariable @Parameter(description = "Goal ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GoalResponse response = goalService.pauseGoal(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Goal paused successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/resume")
    @Operation(summary = "Resume goal")
    public ResponseEntity<ApiResponse<GoalResponse>> resumeGoal(@PathVariable @Parameter(description = "Goal ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GoalResponse response = goalService.resumeGoal(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Goal resumed successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete goal")
    public ResponseEntity<ApiResponse<GoalResponse>> completeGoal(@PathVariable @Parameter(description = "Goal ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GoalResponse response = goalService.completeGoal(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Goal completed successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/fail")
    @Operation(summary = "Mark goal as failed")
    public ResponseEntity<ApiResponse<GoalResponse>> failGoal(@PathVariable @Parameter(description = "Goal ID") UUID id, @RequestBody(required = false) String reason) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GoalResponse response = goalService.failGoal(userId, id, reason);
        return ResponseEntity.ok(ApiResponse.ok(response, "Goal marked as failed", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive goal")
    public ResponseEntity<ApiResponse<GoalResponse>> archiveGoal(@PathVariable @Parameter(description = "Goal ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GoalResponse response = goalService.archiveGoal(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Goal archived successfully", UUID.randomUUID().toString()));
    }

    @PutMapping("/{id}/progress")
    @Operation(summary = "Update goal progress")
    public ResponseEntity<ApiResponse<GoalResponse>> updateProgress(@PathVariable @Parameter(description = "Goal ID") UUID id, @Parameter(description = "Progress percentage") @RequestParam int progress) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GoalResponse response = goalService.updateProgress(userId, id, progress);
        return ResponseEntity.ok(ApiResponse.ok(response, "Progress updated successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/{id}/milestones")
    @Operation(summary = "List goal milestones")
    public ResponseEntity<ApiResponse<List<MilestoneResponse>>> getMilestones(@PathVariable @Parameter(description = "Goal ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<MilestoneResponse> response = goalService.getMilestones(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Milestones retrieved successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/milestones")
    @Operation(summary = "Create goal milestone")
    public ResponseEntity<ApiResponse<MilestoneResponse>> createMilestone(@PathVariable @Parameter(description = "Goal ID") UUID id, @Valid @RequestBody CreateMilestoneRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        MilestoneResponse response = goalService.createMilestone(userId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Milestone created successfully", UUID.randomUUID().toString()));
    }

    @PutMapping("/{id}/milestones/{mid}")
    @Operation(summary = "Update goal milestone")
    public ResponseEntity<ApiResponse<MilestoneResponse>> updateMilestone(@PathVariable @Parameter(description = "Goal ID") UUID id, @PathVariable @Parameter(description = "Milestone ID") UUID mid, @Valid @RequestBody UpdateMilestoneRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        MilestoneResponse response = goalService.updateMilestone(userId, id, mid, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Milestone updated successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/milestones/{mid}/complete")
    @Operation(summary = "Complete goal milestone")
    public ResponseEntity<ApiResponse<MilestoneResponse>> completeMilestone(@PathVariable @Parameter(description = "Goal ID") UUID id, @PathVariable @Parameter(description = "Milestone ID") UUID mid) {
        UUID userId = SecurityUtils.getCurrentUserId();
        MilestoneResponse response = goalService.completeMilestone(userId, id, mid);
        return ResponseEntity.ok(ApiResponse.ok(response, "Milestone completed successfully", UUID.randomUUID().toString()));
    }

    @DeleteMapping("/{id}/milestones/{mid}")
    @Operation(summary = "Delete goal milestone")
    public ResponseEntity<ApiResponse<Void>> deleteMilestone(@PathVariable @Parameter(description = "Goal ID") UUID id, @PathVariable @Parameter(description = "Milestone ID") UUID mid) {
        UUID userId = SecurityUtils.getCurrentUserId();
        goalService.deleteMilestone(userId, id, mid);
        return ResponseEntity.ok(ApiResponse.ok(null, "Milestone deleted successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get goal statistics")
    public ResponseEntity<ApiResponse<GoalStatisticsResponse>> getStatistics() {
        UUID userId = SecurityUtils.getCurrentUserId();
        GoalStatisticsResponse response = goalService.getStatistics(userId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Statistics retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "Get goals by category")
    public ResponseEntity<ApiResponse<List<GoalResponse>>> getGoalsByCategory(@PathVariable @Parameter(description = "Category") String category) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<GoalResponse> response = goalService.getGoalsByCategory(userId, category);
        return ResponseEntity.ok(ApiResponse.ok(response, "Goals retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get goals by status")
    public ResponseEntity<ApiResponse<List<GoalResponse>>> getGoalsByStatus(@PathVariable @Parameter(description = "Goal status") GoalStatus status) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<GoalResponse> response = goalService.getGoalsByStatus(userId, status);
        return ResponseEntity.ok(ApiResponse.ok(response, "Goals retrieved successfully", UUID.randomUUID().toString()));
    }
}
