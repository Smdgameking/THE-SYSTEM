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

    @Mapping(target = "achievementCode", expression = "java(getAchievementCode(userAchievement))")
    @Mapping(target = "achievementName", expression = "java(getAchievementName(userAchievement))")
    @Mapping(target = "category", expression = "java(getAchievementCategory(userAchievement))")
    @Mapping(target = "progressMetadata", source = "progressMetadata", qualifiedByName = "stringToMap")
    UserAchievementResponse toUserAchievementResponse(UserAchievement userAchievement);

    @Mapping(target = "conditions", source = "conditions", qualifiedByName = "stringToMap")
    PolicyResponse toPolicyResponse(XpPolicy policy);

    @Mapping(target = "metadata", source = "metadata", qualifiedByName = "stringToMap")
    RewardHistoryResponse toRewardHistoryResponse(RewardHistory rewardHistory);

    XpAccountResponse toXpAccountResponse(XpAccount account);

    default String getAchievementCode(UserAchievement userAchievement) {
        return "";
    }

    default String getAchievementName(UserAchievement userAchievement) {
        return "";
    }

    default com.thesystem.modules.xp.enums.AchievementCategory getAchievementCategory(UserAchievement userAchievement) {
        return com.thesystem.modules.xp.enums.AchievementCategory.TASK;
    }

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
