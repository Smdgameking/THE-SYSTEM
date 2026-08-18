package com.thesystem.modules.goal.service;

import com.thesystem.modules.goal.dto.CreateGoalRequest;
import com.thesystem.modules.goal.dto.CreateMilestoneRequest;
import com.thesystem.modules.goal.dto.GoalResponse;
import com.thesystem.modules.goal.dto.MilestoneResponse;
import com.thesystem.modules.goal.enums.GoalDifficulty;
import com.thesystem.modules.goal.enums.GoalPriority;
import com.thesystem.modules.goal.enums.GoalStatus;
import com.thesystem.modules.goal.enums.GoalType;
import com.thesystem.modules.goal.enums.GoalVisibility;
import com.thesystem.modules.goal.repository.GoalMilestoneRepository;
import com.thesystem.modules.goal.repository.GoalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:goalservice;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@Transactional
class GoalServiceIntegrationTest {

    @Autowired
    private GoalService goalService;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalMilestoneRepository milestoneRepository;

    private CreateGoalRequest request(String title) {
        return new CreateGoalRequest(
                title,
                "Description for " + title,
                "integration",
                GoalPriority.NORMAL,
                GoalDifficulty.NORMAL,
                GoalType.PROJECT,
                GoalVisibility.PRIVATE,
                100,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void shouldCreateGoalWithGeneratedIdAndPersistIt() {
        UUID userId = UUID.randomUUID();
        GoalResponse created = goalService.createGoal(userId, request("Service Goal"));

        assertThat(created.id()).isNotNull();
        assertThat(created.status()).isEqualTo(GoalStatus.DRAFT);

        GoalResponse found = goalService.getGoals(userId, new GoalService.GoalFilter(null, null, null, null, 0, 20)).get(0);
        assertThat(found.id()).isEqualTo(created.id());
        assertThat(goalRepository.findByIdAndUserIdAndDeletedAtIsNull(created.id(), userId)).isPresent();
    }

    @Test
    void shouldCreateMilestoneWithGeneratedId() {
        UUID userId = UUID.randomUUID();
        GoalResponse goal = goalService.createGoal(userId, request("Goal With Milestone"));

        MilestoneResponse milestone = goalService.createMilestone(
                userId,
                goal.id(),
                new CreateMilestoneRequest("Phase 1", "First phase", 1)
        );

        assertThat(milestone.id()).isNotNull();
        assertThat(milestone.isCompleted()).isFalse();
        assertThat(milestoneRepository.findByIdAndGoalIdAndDeletedAtIsNull(milestone.id(), goal.id())).isPresent();
    }

    @Test
    void shouldCreateGoalWithTagsRoundTrippingJson() {
        UUID userId = UUID.randomUUID();
        CreateGoalRequest tagged = new CreateGoalRequest(
                "Tagged Goal",
                null,
                null,
                GoalPriority.NORMAL,
                GoalDifficulty.NORMAL,
                GoalType.PROJECT,
                GoalVisibility.PRIVATE,
                100,
                null,
                null,
                List.of("verify", "integration"),
                null
        );
        GoalResponse created = goalService.createGoal(userId, tagged);

        assertThat(created.id()).isNotNull();
        assertThat(created.tags()).containsExactly("verify", "integration");
    }

    @Test
    void shouldScopeGoalsByUser() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        goalService.createGoal(userA, request("A Goal"));
        goalService.createGoal(userB, request("B Goal"));

        GoalService.GoalFilter filter = new GoalService.GoalFilter(null, null, null, null, 0, 20);
        List<GoalResponse> aGoals = goalService.getGoals(userA, filter);

        assertThat(aGoals).extracting(GoalResponse::title).containsExactly("A Goal");
    }
}
