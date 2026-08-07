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
import com.thesystem.modules.xp.dto.transaction.TransactionCreateRequest;
import com.thesystem.modules.xp.dto.transaction.TransactionHistoryFilter;
import com.thesystem.modules.xp.dto.transaction.TransactionResponse;
import com.thesystem.modules.xp.dto.xpaccount.XpAccountCreateRequest;
import com.thesystem.modules.xp.dto.xpaccount.XpAccountResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing XP (Experience Points) operations including accounts,
 * transactions, level progression, achievements, policies, rewards, and analytics.
 */
public interface XpService {

    // ========================
    // XP Account Operations
    // ========================

    /**
     * Retrieves the XP account for a given user.
     *
     * @param userId the unique identifier of the user
     * @return the XP account details
     */
    XpAccountResponse getAccount(UUID userId);

    /**
     * Creates a new XP account for the specified user.
     *
     * @param request the account creation request containing user details
     * @return the created XP account details
     */
    XpAccountResponse createAccount(XpAccountCreateRequest request);

    // ========================
    // XP Transaction Operations
    // ========================

    /**
     * Creates a new XP transaction for a user.
     *
     * @param request the transaction creation request
     * @return the created transaction details
     */
    TransactionResponse createTransaction(TransactionCreateRequest request);

    /**
     * Retrieves a specific transaction by its ID.
     *
     * @param transactionId the unique identifier of the transaction
     * @param userId        the unique identifier of the requesting user
     * @return the transaction details
     */
    TransactionResponse getTransaction(UUID transactionId, UUID userId);

    /**
     * Lists transactions for a user with pagination.
     *
     * @param userId   the unique identifier of the user
     * @param pageable pagination and sorting parameters
     * @return a page of transaction details
     */
    Page<TransactionResponse> listTransactions(UUID userId, Pageable pageable);

    /**
     * Retrieves the transaction history for a user based on filters.
     *
     * @param userId   the unique identifier of the user
     * @param filters  the filters to apply to the history
     * @return the filtered list of transactions
     */
    List<TransactionResponse> getTransactionHistory(UUID userId, TransactionHistoryFilter filters);

    // ========================
    // Level System Operations
    // ========================

    /**
     * Calculates the level based on the given amount of XP.
     *
     * @param xp the amount of XP to evaluate
     * @return the calculated level
     */
    int calculateLevel(int xp);

    /**
     * Calculates the progress for a user towards the next level.
     *
     * @param userId the unique identifier of the user
     * @return the progress details including current and next level thresholds
     */
    ProgressResponse calculateProgress(UUID userId);

    /**
     * Retrieves detailed information about a specific level.
     *
     * @param level the level number to retrieve information for
     * @return the level details including thresholds and rewards
     */
    LevelInfo getLevelInfo(int level);

    // ========================
    // Achievement Operations
    // ========================

    /**
     * Retrieves all available achievements.
     *
     * @return a list of all achievement details
     */
    List<AchievementResponse> getAllAchievements();

    /**
     * Retrieves a specific achievement by its ID.
     *
     * @param achievementId the unique identifier of the achievement
     * @return the achievement details
     */
    AchievementResponse getAchievement(UUID achievementId);

    /**
     * Retrieves all achievements unlocked by a specific user.
     *
     * @param userId the unique identifier of the user
     * @return a list of the user's unlocked achievements
     */
    List<UserAchievementResponse> getUserAchievements(UUID userId);

    /**
     * Evaluates and unlocks any achievements the user has earned.
     *
     * @param userId the unique identifier of the user
     * @return a list of newly unlocked achievements
     */
    List<AchievementResponse> checkAchievements(UUID userId);

    /**
     * Manually unlocks an achievement for a user.
     *
     * @param userId        the unique identifier of the user
     * @param achievementId the unique identifier of the achievement to unlock
     * @return the unlocked user achievement details
     */
    UserAchievementResponse unlockAchievement(UUID userId, UUID achievementId);

    // ========================
    // Policy Operations
    // ========================

    /**
     * Retrieves all configured XP policies.
     *
     * @return a list of all policy details
     */
    List<PolicyResponse> getAllPolicies();

    /**
     * Retrieves a specific policy by its ID.
     *
     * @param policyId the unique identifier of the policy
     * @return the policy details
     */
    PolicyResponse getPolicy(UUID policyId);

    /**
     * Creates a new XP policy.
     *
     * @param request the policy creation request
     * @return the created policy details
     */
    PolicyResponse createPolicy(PolicyRequest request);

    /**
     * Updates an existing XP policy.
     *
     * @param policyId the unique identifier of the policy to update
     * @param request  the policy update request
     * @return the updated policy details
     */
    PolicyResponse updatePolicy(UUID policyId, PolicyRequest request);

    /**
     * Deletes an XP policy by its ID.
     *
     * @param policyId the unique identifier of the policy to delete
     */
    void deletePolicy(UUID policyId);

    /**
     * Evaluates all active policies against a user's activity to determine applicable rewards.
     *
     * @param userId the unique identifier of the user
     * @return the policy evaluation results
     */
    PolicyEvaluationResponse evaluatePolicies(UUID userId);

    // ========================
    // Reward Operations
    // ========================

    /**
     * Calculates the reward for a user based on a specific action.
     *
     * @param request the reward calculation request containing user and action details
     * @return the calculated reward details
     */
    RewardResponse calculateReward(RewardCalculationRequest request);

    /**
     * Grants a reward to a user.
     *
     * @param userId  the unique identifier of the user
     * @param rewardId the unique identifier of the reward to grant
     * @return the granted reward details
     */
    RewardResponse grantReward(UUID userId, UUID rewardId);

    /**
     * Retrieves the reward history for a user.
     *
     * @param userId the unique identifier of the user
     * @return a list of the user's reward history
     */
    List<RewardHistoryResponse> getRewardHistory(UUID userId);

    // ========================
    // Analytics Operations
    // ========================

    /**
     * Retrieves overall XP statistics.
     *
     * @return the aggregated statistics
     */
    StatisticsResponse getStatistics();

    /**
     * Retrieves the XP leaderboard.
     *
     * @param pageable pagination and sorting parameters
     * @return the leaderboard with ranked entries
     */
    LeaderboardResponse getLeaderboard(Pageable pageable);
}
