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
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GoalResponse>> createGoal(@Valid @RequestBody CreateGoalRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GoalResponse response = goalService.createGoal(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Goal created successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GoalDetailResponse>> getGoal(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GoalDetailResponse response = goalService.getGoal(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Goal retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<GoalResponse>>> getGoals(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
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
    public ResponseEntity<ApiResponse<GoalResponse>> updateGoal(@PathVariable UUID id, @Valid @RequestBody UpdateGoalRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GoalResponse response = goalService.updateGoal(userId, id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Goal updated successfully", UUID.randomUUID().toString()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGoal(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        goalService.deleteGoal(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Goal deleted successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<GoalResponse>> startGoal(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GoalResponse response = goalService.startGoal(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Goal started successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<ApiResponse<GoalResponse>> pauseGoal(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GoalResponse response = goalService.pauseGoal(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Goal paused successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<ApiResponse<GoalResponse>> resumeGoal(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GoalResponse response = goalService.resumeGoal(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Goal resumed successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<GoalResponse>> completeGoal(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GoalResponse response = goalService.completeGoal(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Goal completed successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<ApiResponse<GoalResponse>> failGoal(@PathVariable UUID id, @RequestBody(required = false) String reason) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GoalResponse response = goalService.failGoal(userId, id, reason);
        return ResponseEntity.ok(ApiResponse.ok(response, "Goal marked as failed", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<GoalResponse>> archiveGoal(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GoalResponse response = goalService.archiveGoal(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Goal archived successfully", UUID.randomUUID().toString()));
    }

    @PutMapping("/{id}/progress")
    public ResponseEntity<ApiResponse<GoalResponse>> updateProgress(@PathVariable UUID id, @RequestParam int progress) {
        UUID userId = SecurityUtils.getCurrentUserId();
        GoalResponse response = goalService.updateProgress(userId, id, progress);
        return ResponseEntity.ok(ApiResponse.ok(response, "Progress updated successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/{id}/milestones")
    public ResponseEntity<ApiResponse<List<MilestoneResponse>>> getMilestones(@PathVariable UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<MilestoneResponse> response = goalService.getMilestones(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Milestones retrieved successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/milestones")
    public ResponseEntity<ApiResponse<MilestoneResponse>> createMilestone(@PathVariable UUID id, @Valid @RequestBody CreateMilestoneRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        MilestoneResponse response = goalService.createMilestone(userId, id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Milestone created successfully", UUID.randomUUID().toString()));
    }

    @PutMapping("/{id}/milestones/{mid}")
    public ResponseEntity<ApiResponse<MilestoneResponse>> updateMilestone(@PathVariable UUID id, @PathVariable UUID mid, @Valid @RequestBody UpdateMilestoneRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        MilestoneResponse response = goalService.updateMilestone(userId, id, mid, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Milestone updated successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/{id}/milestones/{mid}/complete")
    public ResponseEntity<ApiResponse<MilestoneResponse>> completeMilestone(@PathVariable UUID id, @PathVariable UUID mid) {
        UUID userId = SecurityUtils.getCurrentUserId();
        MilestoneResponse response = goalService.completeMilestone(userId, id, mid);
        return ResponseEntity.ok(ApiResponse.ok(response, "Milestone completed successfully", UUID.randomUUID().toString()));
    }

    @DeleteMapping("/{id}/milestones/{mid}")
    public ResponseEntity<ApiResponse<Void>> deleteMilestone(@PathVariable UUID id, @PathVariable UUID mid) {
        UUID userId = SecurityUtils.getCurrentUserId();
        goalService.deleteMilestone(userId, id, mid);
        return ResponseEntity.ok(ApiResponse.ok(null, "Milestone deleted successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<GoalStatisticsResponse>> getStatistics() {
        UUID userId = SecurityUtils.getCurrentUserId();
        GoalStatisticsResponse response = goalService.getStatistics(userId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Statistics retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<GoalResponse>>> getGoalsByCategory(@PathVariable String category) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<GoalResponse> response = goalService.getGoalsByCategory(userId, category);
        return ResponseEntity.ok(ApiResponse.ok(response, "Goals retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<GoalResponse>>> getGoalsByStatus(@PathVariable GoalStatus status) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<GoalResponse> response = goalService.getGoalsByStatus(userId, status);
        return ResponseEntity.ok(ApiResponse.ok(response, "Goals retrieved successfully", UUID.randomUUID().toString()));
    }
}
