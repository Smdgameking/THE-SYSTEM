package com.thesystem.modules.xp.mapper;

import com.thesystem.modules.xp.dto.achievement.UserAchievementResponse;
import com.thesystem.modules.xp.entity.AchievementDefinition;
import com.thesystem.modules.xp.entity.UserAchievement;
import com.thesystem.modules.xp.enums.AchievementCategory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class XpMapperTest {

    private final XpMapper xpMapper = new XpMapperImpl();

    @Test
    void shouldMapAchievementCodeFromDefinition() {
        AchievementDefinition definition = new AchievementDefinition();
        definition.setId(UUID.randomUUID());
        definition.setCode("FIRST_TASK");
        definition.setName("First Task");
        definition.setCategory(AchievementCategory.TASK);

        UserAchievement userAchievement = createUserAchievement(definition.getId());

        UserAchievementResponse response = xpMapper.toUserAchievementResponse(userAchievement, definition);

        assertThat(response).isNotNull();
        assertThat(response.achievementCode()).isEqualTo("FIRST_TASK");
    }

    @Test
    void shouldMapAchievementNameFromDefinition() {
        AchievementDefinition definition = new AchievementDefinition();
        definition.setId(UUID.randomUUID());
        definition.setCode("FIRST_TASK");
        definition.setName("First Task");
        definition.setCategory(AchievementCategory.TASK);

        UserAchievement userAchievement = createUserAchievement(definition.getId());

        UserAchievementResponse response = xpMapper.toUserAchievementResponse(userAchievement, definition);

        assertThat(response).isNotNull();
        assertThat(response.achievementName()).isEqualTo("First Task");
    }

    @Test
    void shouldMapCategoryFromDefinition() {
        AchievementDefinition definition = new AchievementDefinition();
        definition.setId(UUID.randomUUID());
        definition.setCode("STREAK_3");
        definition.setName("Streak 3");
        definition.setCategory(AchievementCategory.STREAK);

        UserAchievement userAchievement = createUserAchievement(definition.getId());

        UserAchievementResponse response = xpMapper.toUserAchievementResponse(userAchievement, definition);

        assertThat(response).isNotNull();
        assertThat(response.category()).isEqualTo(AchievementCategory.STREAK);
    }

    @Test
    void shouldNotAlwaysReturnTaskCategory() {
        AchievementDefinition streakDefinition = new AchievementDefinition();
        streakDefinition.setId(UUID.randomUUID());
        streakDefinition.setCode("STREAK_3");
        streakDefinition.setName("Streak 3");
        streakDefinition.setCategory(AchievementCategory.STREAK);

        AchievementDefinition rewardDefinition = new AchievementDefinition();
        rewardDefinition.setId(UUID.randomUUID());
        rewardDefinition.setCode("FIRST_REWARD");
        rewardDefinition.setName("First Reward");
        rewardDefinition.setCategory(AchievementCategory.SPECIAL);

        UserAchievement streakAchievement = createUserAchievement(streakDefinition.getId());
        UserAchievement rewardAchievement = createUserAchievement(rewardDefinition.getId());

        UserAchievementResponse streakResponse = xpMapper.toUserAchievementResponse(streakAchievement, streakDefinition);
        UserAchievementResponse rewardResponse = xpMapper.toUserAchievementResponse(rewardAchievement, rewardDefinition);

        assertThat(streakResponse.category()).isEqualTo(AchievementCategory.STREAK);
        assertThat(rewardResponse.category()).isEqualTo(AchievementCategory.SPECIAL);
        assertThat(streakResponse.category()).isNotEqualTo(rewardResponse.category());
    }

    @Test
    void shouldMapDifferentDefinitionsToDifferentMetadata() {
        AchievementDefinition definitionA = new AchievementDefinition();
        definitionA.setId(UUID.randomUUID());
        definitionA.setCode("CODE_A");
        definitionA.setName("Name A");
        definitionA.setCategory(AchievementCategory.TASK);

        AchievementDefinition definitionB = new AchievementDefinition();
        definitionB.setId(UUID.randomUUID());
        definitionB.setCode("CODE_B");
        definitionB.setName("Name B");
        definitionB.setCategory(AchievementCategory.STREAK);

        UserAchievement userAchievementA = createUserAchievement(definitionA.getId());
        UserAchievement userAchievementB = createUserAchievement(definitionB.getId());

        UserAchievementResponse responseA = xpMapper.toUserAchievementResponse(userAchievementA, definitionA);
        UserAchievementResponse responseB = xpMapper.toUserAchievementResponse(userAchievementB, definitionB);

        assertThat(responseA.achievementCode()).isEqualTo("CODE_A");
        assertThat(responseB.achievementCode()).isEqualTo("CODE_B");
        assertThat(responseA.achievementName()).isEqualTo("Name A");
        assertThat(responseB.achievementName()).isEqualTo("Name B");
        assertThat(responseA.category()).isEqualTo(AchievementCategory.TASK);
        assertThat(responseB.category()).isEqualTo(AchievementCategory.STREAK);
    }

    @Test
    void shouldReturnNullWhenBothSourcesAreNull() {
        UserAchievementResponse response = xpMapper.toUserAchievementResponse(null, null);

        assertThat(response).isNull();
    }

    @Test
    void shouldReturnResponseWithDefinitionFieldsWhenUserAchievementIsNull() {
        AchievementDefinition definition = new AchievementDefinition();
        definition.setId(UUID.randomUUID());
        definition.setCode("TEST");
        definition.setName("Test");
        definition.setCategory(AchievementCategory.TASK);

        UserAchievementResponse response = xpMapper.toUserAchievementResponse(null, definition);

        assertThat(response).isNotNull();
        assertThat(response.achievementCode()).isEqualTo("TEST");
        assertThat(response.achievementName()).isEqualTo("Test");
        assertThat(response.category()).isEqualTo(AchievementCategory.TASK);
    }

    @Test
    void shouldHandleNullDefinitionSafely() {
        UserAchievement userAchievement = createUserAchievement(UUID.randomUUID());

        UserAchievementResponse response = xpMapper.toUserAchievementResponse(userAchievement, null);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userAchievement.getId());
        assertThat(response.userId()).isEqualTo(userAchievement.getUserId());
        assertThat(response.achievementCode()).isNull();
        assertThat(response.achievementName()).isNull();
        assertThat(response.category()).isNull();
    }

    @Test
    void shouldPreserveBackwardCompatibleOverload() {
        AchievementDefinition definition = new AchievementDefinition();
        definition.setId(UUID.randomUUID());
        definition.setCode("BACKWARD");
        definition.setName("Backward");
        definition.setCategory(AchievementCategory.TASK);

        UserAchievement userAchievement = createUserAchievement(definition.getId());

        UserAchievementResponse response = xpMapper.toUserAchievementResponse(userAchievement);

        assertThat(response).isNotNull();
        assertThat(response.achievementCode()).isNull();
        assertThat(response.achievementName()).isNull();
        assertThat(response.category()).isNull();
    }

    private UserAchievement createUserAchievement(UUID achievementId) {
        UserAchievement userAchievement = new UserAchievement();
        userAchievement.setId(UUID.randomUUID());
        userAchievement.setUserId(UUID.randomUUID());
        userAchievement.setAchievementId(achievementId);
        userAchievement.setCurrentProgress(1);
        userAchievement.setTargetProgress(1);
        userAchievement.setIsUnlocked(true);
        userAchievement.setUnlockedAt(Instant.now());
        userAchievement.setCreatedAt(Instant.now());
        return userAchievement;
    }
}
