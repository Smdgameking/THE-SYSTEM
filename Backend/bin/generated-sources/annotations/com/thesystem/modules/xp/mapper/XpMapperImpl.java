package com.thesystem.modules.xp.mapper;

import com.thesystem.modules.xp.dto.achievement.AchievementResponse;
import com.thesystem.modules.xp.dto.achievement.UserAchievementResponse;
import com.thesystem.modules.xp.dto.policy.PolicyResponse;
import com.thesystem.modules.xp.dto.reward.RewardHistoryResponse;
import com.thesystem.modules.xp.dto.transaction.TransactionResponse;
import com.thesystem.modules.xp.dto.xpaccount.XpAccountResponse;
import com.thesystem.modules.xp.entity.AchievementDefinition;
import com.thesystem.modules.xp.entity.RewardHistory;
import com.thesystem.modules.xp.entity.UserAchievement;
import com.thesystem.modules.xp.entity.XpAccount;
import com.thesystem.modules.xp.entity.XpPolicy;
import com.thesystem.modules.xp.entity.XpTransaction;
import com.thesystem.modules.xp.enums.AchievementCategory;
import com.thesystem.modules.xp.enums.PolicyType;
import com.thesystem.modules.xp.enums.RequirementType;
import com.thesystem.modules.xp.enums.TransactionType;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-09T03:54:43+0530",
    comments = "version: 1.6.3, compiler: Eclipse JDT (IDE) 3.46.100.v20260624-0231, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class XpMapperImpl implements XpMapper {

    @Override
    public TransactionResponse toTransactionResponse(XpTransaction transaction) {
        if ( transaction == null ) {
            return null;
        }

        Map<String, Object> metadata = null;
        UUID id = null;
        UUID userId = null;
        TransactionType transactionType = null;
        Integer amount = null;
        Integer balanceAfter = null;
        String sourceEngine = null;
        UUID sourceId = null;
        String sourceType = null;
        UUID policyId = null;
        Double multiplierApplied = null;
        Integer baseAmount = null;
        String reason = null;
        Instant createdAt = null;

        metadata = stringToMap( transaction.getMetadata() );
        id = transaction.getId();
        userId = transaction.getUserId();
        transactionType = transaction.getTransactionType();
        amount = transaction.getAmount();
        balanceAfter = transaction.getBalanceAfter();
        sourceEngine = transaction.getSourceEngine();
        sourceId = transaction.getSourceId();
        sourceType = transaction.getSourceType();
        policyId = transaction.getPolicyId();
        multiplierApplied = transaction.getMultiplierApplied();
        baseAmount = transaction.getBaseAmount();
        reason = transaction.getReason();
        createdAt = transaction.getCreatedAt();

        TransactionResponse transactionResponse = new TransactionResponse( id, userId, transactionType, amount, balanceAfter, sourceEngine, sourceId, sourceType, policyId, multiplierApplied, baseAmount, reason, metadata, createdAt );

        return transactionResponse;
    }

    @Override
    public AchievementResponse toAchievementResponse(AchievementDefinition definition) {
        if ( definition == null ) {
            return null;
        }

        Map<String, Object> requirementValue = null;
        UUID id = null;
        String code = null;
        String name = null;
        String description = null;
        AchievementCategory category = null;
        String iconUrl = null;
        RequirementType requirementType = null;
        Integer xpReward = null;
        Boolean isHidden = null;
        Boolean isRepeatable = null;
        Integer sortOrder = null;
        Instant createdAt = null;

        requirementValue = stringToMap( definition.getRequirementValue() );
        id = definition.getId();
        code = definition.getCode();
        name = definition.getName();
        description = definition.getDescription();
        category = definition.getCategory();
        iconUrl = definition.getIconUrl();
        requirementType = definition.getRequirementType();
        xpReward = definition.getXpReward();
        isHidden = definition.getIsHidden();
        isRepeatable = definition.getIsRepeatable();
        sortOrder = definition.getSortOrder();
        createdAt = definition.getCreatedAt();

        AchievementResponse achievementResponse = new AchievementResponse( id, code, name, description, category, iconUrl, requirementType, requirementValue, xpReward, isHidden, isRepeatable, sortOrder, createdAt );

        return achievementResponse;
    }

    @Override
    public UserAchievementResponse toUserAchievementResponse(UserAchievement userAchievement) {
        if ( userAchievement == null ) {
            return null;
        }

        Map<String, Object> progressMetadata = null;
        UUID id = null;
        UUID userId = null;
        UUID achievementId = null;
        Integer currentProgress = null;
        Integer targetProgress = null;
        Boolean isUnlocked = null;
        Instant unlockedAt = null;
        Instant createdAt = null;

        progressMetadata = stringToMap( userAchievement.getProgressMetadata() );
        id = userAchievement.getId();
        userId = userAchievement.getUserId();
        achievementId = userAchievement.getAchievementId();
        currentProgress = userAchievement.getCurrentProgress();
        targetProgress = userAchievement.getTargetProgress();
        isUnlocked = userAchievement.getIsUnlocked();
        unlockedAt = userAchievement.getUnlockedAt();
        createdAt = userAchievement.getCreatedAt();

        String achievementCode = getAchievementCode(userAchievement);
        String achievementName = getAchievementName(userAchievement);
        AchievementCategory category = getAchievementCategory(userAchievement);

        UserAchievementResponse userAchievementResponse = new UserAchievementResponse( id, userId, achievementId, achievementCode, achievementName, category, currentProgress, targetProgress, isUnlocked, unlockedAt, progressMetadata, createdAt );

        return userAchievementResponse;
    }

    @Override
    public PolicyResponse toPolicyResponse(XpPolicy policy) {
        if ( policy == null ) {
            return null;
        }

        Map<String, Object> conditions = null;
        UUID id = null;
        String code = null;
        String name = null;
        String description = null;
        PolicyType policyType = null;
        Integer baseXp = null;
        Double multiplier = null;
        Boolean isActive = null;
        Integer priority = null;
        Instant createdAt = null;
        Instant updatedAt = null;

        conditions = stringToMap( policy.getConditions() );
        id = policy.getId();
        code = policy.getCode();
        name = policy.getName();
        description = policy.getDescription();
        policyType = policy.getPolicyType();
        baseXp = policy.getBaseXp();
        multiplier = policy.getMultiplier();
        isActive = policy.getIsActive();
        priority = policy.getPriority();
        createdAt = policy.getCreatedAt();
        updatedAt = policy.getUpdatedAt();

        PolicyResponse policyResponse = new PolicyResponse( id, code, name, description, policyType, baseXp, multiplier, conditions, isActive, priority, createdAt, updatedAt );

        return policyResponse;
    }

    @Override
    public RewardHistoryResponse toRewardHistoryResponse(RewardHistory rewardHistory) {
        if ( rewardHistory == null ) {
            return null;
        }

        Map<String, Object> metadata = null;
        UUID id = null;
        UUID userId = null;
        String rewardType = null;
        String sourceType = null;
        UUID sourceId = null;
        Integer xpAmount = null;
        UUID policyId = null;
        Double multiplierApplied = null;
        Integer baseAmount = null;
        Instant awardedAt = null;
        Instant createdAt = null;

        metadata = stringToMap( rewardHistory.getMetadata() );
        id = rewardHistory.getId();
        userId = rewardHistory.getUserId();
        if ( rewardHistory.getRewardType() != null ) {
            rewardType = rewardHistory.getRewardType().name();
        }
        sourceType = rewardHistory.getSourceType();
        sourceId = rewardHistory.getSourceId();
        xpAmount = rewardHistory.getXpAmount();
        policyId = rewardHistory.getPolicyId();
        multiplierApplied = rewardHistory.getMultiplierApplied();
        baseAmount = rewardHistory.getBaseAmount();
        awardedAt = rewardHistory.getAwardedAt();
        createdAt = rewardHistory.getCreatedAt();

        RewardHistoryResponse rewardHistoryResponse = new RewardHistoryResponse( id, userId, rewardType, sourceType, sourceId, xpAmount, policyId, multiplierApplied, baseAmount, awardedAt, metadata, createdAt );

        return rewardHistoryResponse;
    }

    @Override
    public XpAccountResponse toXpAccountResponse(XpAccount account) {
        if ( account == null ) {
            return null;
        }

        UUID id = null;
        UUID userId = null;
        Integer currentXp = null;
        Integer currentLevel = null;
        Integer totalXpEarned = null;
        Integer totalXpSpent = null;
        Integer lifetimeXp = null;
        Double levelProgress = null;
        Instant createdAt = null;
        Instant updatedAt = null;

        id = account.getId();
        userId = account.getUserId();
        currentXp = account.getCurrentXp();
        currentLevel = account.getCurrentLevel();
        totalXpEarned = account.getTotalXpEarned();
        totalXpSpent = account.getTotalXpSpent();
        lifetimeXp = account.getLifetimeXp();
        levelProgress = account.getLevelProgress();
        createdAt = account.getCreatedAt();
        updatedAt = account.getUpdatedAt();

        XpAccountResponse xpAccountResponse = new XpAccountResponse( id, userId, currentXp, currentLevel, totalXpEarned, totalXpSpent, lifetimeXp, levelProgress, createdAt, updatedAt );

        return xpAccountResponse;
    }
}
