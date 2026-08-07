package com.thesystem.modules.xp.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thesystem.modules.xp.dto.AchievementDefinitionResponse;
import com.thesystem.modules.xp.dto.RewardHistoryResponse;
import com.thesystem.modules.xp.dto.UserAchievementResponse;
import com.thesystem.modules.xp.dto.XpAccountResponse;
import com.thesystem.modules.xp.dto.XpPolicyResponse;
import com.thesystem.modules.xp.dto.XpTransactionResponse;
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
    XpTransactionResponse toXpTransactionResponse(XpTransaction transaction);

    @Mapping(target = "requirementValue", source = "requirementValue", qualifiedByName = "stringToMap")
    AchievementDefinitionResponse toAchievementDefinitionResponse(AchievementDefinition definition);

    @Mapping(target = "progressMetadata", source = "progressMetadata", qualifiedByName = "stringToMap")
    UserAchievementResponse toUserAchievementResponse(UserAchievement userAchievement);

    @Mapping(target = "conditions", source = "conditions", qualifiedByName = "stringToMap")
    XpPolicyResponse toXpPolicyResponse(XpPolicy policy);

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
