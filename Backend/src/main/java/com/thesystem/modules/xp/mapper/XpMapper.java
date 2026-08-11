package com.thesystem.modules.xp.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesystem.modules.xp.dto.achievement.AchievementResponse;
import com.thesystem.modules.xp.dto.achievement.UserAchievementResponse;
import com.thesystem.modules.xp.dto.level.LevelInfo;
import com.thesystem.modules.xp.dto.level.ProgressResponse;
import com.thesystem.modules.xp.dto.policy.PolicyEvaluationResponse;
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
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Map;

@Mapper(componentModel = "spring")
public interface XpMapper {

    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "stringToMap")
    TransactionResponse toTransactionResponse(XpTransaction transaction);

    @Mapping(target = "requirementValue", source = "requirementValue", qualifiedByName = "stringToMap")
    AchievementResponse toAchievementResponse(AchievementDefinition definition);

    @Mapping(target = "achievementCode", source = "definition.code")
    @Mapping(target = "achievementName", source = "definition.name")
    @Mapping(target = "category", source = "definition.category")
    @Mapping(target = "progressMetadata", source = "userAchievement.progressMetadata", qualifiedByName = "stringToMap")
    @Mapping(target = "id", source = "userAchievement.id")
    @Mapping(target = "userId", source = "userAchievement.userId")
    @Mapping(target = "achievementId", source = "userAchievement.achievementId")
    @Mapping(target = "currentProgress", source = "userAchievement.currentProgress")
    @Mapping(target = "targetProgress", source = "userAchievement.targetProgress")
    @Mapping(target = "isUnlocked", source = "userAchievement.isUnlocked")
    @Mapping(target = "unlockedAt", source = "userAchievement.unlockedAt")
    @Mapping(target = "createdAt", source = "userAchievement.createdAt")
    UserAchievementResponse toUserAchievementResponse(UserAchievement userAchievement, AchievementDefinition definition);

    default UserAchievementResponse toUserAchievementResponse(UserAchievement userAchievement) {
        return toUserAchievementResponse(userAchievement, null);
    }

    @Mapping(target = "conditions", source = "conditions", qualifiedByName = "stringToMap")
    PolicyResponse toPolicyResponse(XpPolicy policy);

    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "stringToMap")
    RewardHistoryResponse toRewardHistoryResponse(RewardHistory rewardHistory);

    XpAccountResponse toXpAccountResponse(XpAccount account);

    @Named("stringToMap")
    default Map<String, Object> stringToMap(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(value, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    default String mapToString(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return "{}";
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }
}
