package com.thesystem.modules.xp.service;

import com.thesystem.modules.xp.dto.achievement.AchievementResponse;
import com.thesystem.modules.xp.dto.achievement.UserAchievementResponse;
import com.thesystem.modules.xp.dto.level.LevelInfo;
import com.thesystem.modules.xp.dto.level.ProgressResponse;
import com.thesystem.modules.xp.dto.policy.PolicyEvaluationResponse;
import com.thesystem.modules.xp.dto.policy.PolicyRequest;
import com.thesystem.modules.xp.dto.policy.PolicyResponse;
import com.thesystem.modules.xp.dto.reward.RewardCalculationRequest;
import com.thesystem.modules.xp.dto.reward.RewardHistoryResponse;
import com.thesystem.modules.xp.dto.reward.RewardResponse;
import com.thesystem.modules.xp.dto.statistics.LeaderboardEntry;
import com.thesystem.modules.xp.dto.statistics.LeaderboardResponse;
import com.thesystem.modules.xp.dto.statistics.StatisticsResponse;
import com.thesystem.modules.xp.dto.streak.UserStreakResponse;
import com.thesystem.modules.xp.dto.transaction.TransactionCreateRequest;
import com.thesystem.modules.xp.dto.transaction.TransactionHistoryFilter;
import com.thesystem.modules.xp.dto.transaction.TransactionResponse;
import com.thesystem.modules.xp.dto.xpaccount.XpAccountCreateRequest;
import com.thesystem.modules.xp.dto.xpaccount.XpAccountResponse;
import com.thesystem.modules.xp.enums.PolicyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for managing XP (Experience Points) operations including accounts,
 * transactions, level progression, achievements, policies, rewards, and analytics.
 */
public interface XpService {

    // ========================
    // XP Account Operations
    // ========================

    XpAccountResponse getAccount(UUID userId);

    XpAccountResponse createAccount(XpAccountCreateRequest request);

    // ========================
    // XP Transaction Operations
    // ========================

    TransactionResponse createTransaction(TransactionCreateRequest request);

    TransactionResponse createTransaction(UUID userId, TransactionCreateRequest request);

    TransactionResponse createTransaction(UUID userId, TransactionCreateRequest request, UUID policyId, Double multiplierApplied, Integer baseAmount);

    double calculatePolicyMultiplier(UUID userId, Map<String, Object> context);

    record XpCalculationResult(UUID primaryPolicyId, int baseXp, double multiplier, int finalXp) {
    }

    XpCalculationResult calculateXpForEvent(UUID userId, Map<String, Object> context, XpSourceType sourceType);

    TransactionResponse getTransaction(UUID transactionId, UUID userId);

    Page<TransactionResponse> listTransactions(UUID userId, Pageable pageable);

    List<TransactionResponse> getTransactionHistory(UUID userId, TransactionHistoryFilter filters);

    // ========================
    // Level System Operations
    // ========================

    int calculateLevel(int xp);

    ProgressResponse calculateProgress(UUID userId);

    LevelInfo getLevelInfo(int level);

    // ========================
    // Achievement Operations
    // ========================

    List<AchievementResponse> getAllAchievements();

    AchievementResponse getAchievement(UUID achievementId);

    List<UserAchievementResponse> getUserAchievements(UUID userId);

    UserAchievementResponse getUserAchievement(UUID userId, UUID userAchievementId);

    List<AchievementResponse> checkAchievements(UUID userId);

    UserAchievementResponse unlockAchievement(UUID userId, UUID achievementId);

    // ========================
    // Streak Operations
    // ========================

    UserStreakResponse getUserStreak(UUID userId);

    /**
     * Returns the count of qualifying activity records per day for the user
     * within the inclusive-exclusive window {@code [from, to)}.
     *
     * @param userId the ID of the user
     * @param from   the window start (inclusive)
     * @param to     the window end (exclusive)
     * @return the per-day activity counts, ordered by date ascending
     */
    List<ActivityDay> getActivityTrend(UUID userId, LocalDate from, LocalDate to);

    record ActivityDay(LocalDate date, long count) {
    }

    // ========================
    // Policy Operations
    // ========================

    List<PolicyResponse> getAllPolicies();

    PolicyResponse getPolicy(UUID policyId);

    PolicyResponse createPolicy(PolicyRequest request);

    PolicyResponse updatePolicy(UUID policyId, PolicyRequest request);

    void deletePolicy(UUID policyId);

    PolicyEvaluationResponse evaluatePolicies(UUID userId);

    // ========================
    // Reward Operations
    // ========================

    RewardResponse calculateReward(RewardCalculationRequest request);

    RewardResponse grantReward(UUID userId, UUID rewardId);

    List<RewardHistoryResponse> getRewardHistory(UUID userId);

    // ========================
    // Analytics Operations
    // ========================

    StatisticsResponse getStatistics();

    LeaderboardResponse getLeaderboard(Pageable pageable);

    // ========================
    // Source Type Mapping
    // ========================

    enum XpSourceType {
        TASK(PolicyType.TASK_COMPLETION),
        GOAL(PolicyType.GOAL_COMPLETION),
        REWARD(PolicyType.BONUS),
        MANUAL(null);

        private final PolicyType basePolicyType;

        XpSourceType(PolicyType basePolicyType) {
            this.basePolicyType = basePolicyType;
        }

        public PolicyType getBasePolicyType() {
            return basePolicyType;
        }
    }
}
