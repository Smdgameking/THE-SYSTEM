package com.thesystem.modules.xp.controller;

import com.thesystem.common.response.ApiResponse;
import com.thesystem.modules.xp.dto.achievement.AchievementResponse;
import com.thesystem.modules.xp.dto.achievement.UserAchievementResponse;
import com.thesystem.modules.xp.dto.policy.PolicyRequest;
import com.thesystem.modules.xp.dto.policy.PolicyResponse;
import com.thesystem.modules.xp.dto.statistics.LeaderboardResponse;
import com.thesystem.modules.xp.dto.statistics.StatisticsResponse;
import com.thesystem.modules.xp.dto.transaction.TransactionResponse;
import com.thesystem.modules.xp.dto.xpaccount.XpAccountResponse;
import com.thesystem.modules.xp.exception.XpException;
import com.thesystem.modules.xp.service.XpService;
import com.thesystem.security.util.SecurityUtils;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/xp")
@Tag(name = "XP")
public class XpController {

    private final XpService xpService;

    public XpController(XpService xpService) {
        this.xpService = xpService;
    }

    @GetMapping("/account")
    @Operation(summary = "Get current user XP account")
    public ResponseEntity<ApiResponse<XpAccountResponse>> getAccount() {
        UUID userId = SecurityUtils.getCurrentUserId();
        XpAccountResponse response = xpService.getAccount(userId);
        return ResponseEntity.ok(ApiResponse.ok(response, "XP account retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/transactions")
    @Operation(summary = "List user XP transactions")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactions(Pageable pageable) {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<TransactionResponse> response = xpService.listTransactions(userId, pageable).getContent();
        return ResponseEntity.ok(ApiResponse.ok(response, "Transactions retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/transactions/{id}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransaction(
            @PathVariable @Parameter(description = "Transaction ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        TransactionResponse response = xpService.getTransaction(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Transaction retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get XP statistics")
    public ResponseEntity<ApiResponse<StatisticsResponse>> getStatistics() {
        StatisticsResponse response = xpService.getStatistics();
        return ResponseEntity.ok(ApiResponse.ok(response, "Statistics retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Get XP leaderboard")
    public ResponseEntity<ApiResponse<LeaderboardResponse>> getLeaderboard(Pageable pageable) {
        LeaderboardResponse response = xpService.getLeaderboard(pageable);
        return ResponseEntity.ok(ApiResponse.ok(response, "Leaderboard retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/achievements")
    @Operation(summary = "List all achievements")
    public ResponseEntity<ApiResponse<List<AchievementResponse>>> getAchievements() {
        List<AchievementResponse> response = xpService.getAllAchievements();
        return ResponseEntity.ok(ApiResponse.ok(response, "Achievements retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/achievements/{id}")
    @Operation(summary = "Get achievement by ID")
    public ResponseEntity<ApiResponse<AchievementResponse>> getAchievement(
            @PathVariable @Parameter(description = "Achievement ID") UUID id) {
        AchievementResponse response = xpService.getAchievement(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Achievement retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/achievements/user")
    @Operation(summary = "Get current user achievements")
    public ResponseEntity<ApiResponse<List<UserAchievementResponse>>> getUserAchievements() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<UserAchievementResponse> response = xpService.getUserAchievements(userId);
        return ResponseEntity.ok(ApiResponse.ok(response, "User achievements retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/achievements/user/{id}")
    @Operation(summary = "Get user achievement by ID")
    public ResponseEntity<ApiResponse<UserAchievementResponse>> getUserAchievement(
            @PathVariable @Parameter(description = "User Achievement ID") UUID id) {
        UUID userId = SecurityUtils.getCurrentUserId();
        UserAchievementResponse response = xpService.unlockAchievement(userId, id);
        return ResponseEntity.ok(ApiResponse.ok(response, "User achievement retrieved successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/achievements/check")
    @Operation(summary = "Check and unlock achievements")
    public ResponseEntity<ApiResponse<List<AchievementResponse>>> checkAchievements() {
        UUID userId = SecurityUtils.getCurrentUserId();
        List<AchievementResponse> response = xpService.checkAchievements(userId);
        return ResponseEntity.ok(ApiResponse.ok(response, "Achievements checked successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/policies")
    @Operation(summary = "List all XP policies")
    public ResponseEntity<ApiResponse<List<PolicyResponse>>> getPolicies() {
        List<PolicyResponse> response = xpService.getAllPolicies();
        return ResponseEntity.ok(ApiResponse.ok(response, "Policies retrieved successfully", UUID.randomUUID().toString()));
    }

    @GetMapping("/policies/{id}")
    @Operation(summary = "Get policy by ID")
    public ResponseEntity<ApiResponse<PolicyResponse>> getPolicy(
            @PathVariable @Parameter(description = "Policy ID") UUID id) {
        PolicyResponse response = xpService.getPolicy(id);
        return ResponseEntity.ok(ApiResponse.ok(response, "Policy retrieved successfully", UUID.randomUUID().toString()));
    }

    @PostMapping("/policies")
    @Operation(summary = "Create XP policy (admin)")
    public ResponseEntity<ApiResponse<PolicyResponse>> createPolicy(
            @Valid @RequestBody PolicyRequest request) {
        if (!SecurityUtils.isAdmin()) {
            throw new XpException("Admin access required", "FORBIDDEN", 403);
        }
        PolicyResponse response = xpService.createPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Policy created successfully", UUID.randomUUID().toString()));
    }

    @PatchMapping("/policies/{id}")
    @Operation(summary = "Update XP policy (admin)")
    public ResponseEntity<ApiResponse<PolicyResponse>> updatePolicy(
            @PathVariable @Parameter(description = "Policy ID") UUID id,
            @Valid @RequestBody PolicyRequest request) {
        if (!SecurityUtils.isAdmin()) {
            throw new XpException("Admin access required", "FORBIDDEN", 403);
        }
        PolicyResponse response = xpService.updatePolicy(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Policy updated successfully", UUID.randomUUID().toString()));
    }

    @DeleteMapping("/policies/{id}")
    @Operation(summary = "Delete XP policy (admin)")
    public ResponseEntity<ApiResponse<Void>> deletePolicy(
            @PathVariable @Parameter(description = "Policy ID") UUID id) {
        if (!SecurityUtils.isAdmin()) {
            throw new XpException("Admin access required", "FORBIDDEN", 403);
        }
        xpService.deletePolicy(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Policy deleted successfully", UUID.randomUUID().toString()));
    }
}
