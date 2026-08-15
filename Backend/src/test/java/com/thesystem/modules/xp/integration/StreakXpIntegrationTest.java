package com.thesystem.modules.xp.integration;

import com.thesystem.modules.auth.entity.User;
import com.thesystem.modules.auth.repository.UserRepository;
import com.thesystem.modules.goal.entity.Goal;
import com.thesystem.modules.goal.repository.GoalRepository;
import com.thesystem.modules.goal.service.GoalService;
import com.thesystem.modules.task.entity.Task;
import com.thesystem.modules.task.repository.TaskRepository;
import com.thesystem.modules.task.service.TaskService;
import com.thesystem.modules.task.events.TaskCompletedEvent;
import com.thesystem.modules.user.entity.UserProfile;
import com.thesystem.modules.user.repository.UserProfileRepository;
import com.thesystem.modules.xp.entity.XpAccount;
import com.thesystem.modules.xp.entity.XpPolicy;
import com.thesystem.modules.xp.entity.XpTransaction;
import com.thesystem.modules.xp.entity.UserStreak;
import com.thesystem.modules.xp.entity.UserStreakHistory;
import com.thesystem.modules.xp.enums.PolicyType;
import com.thesystem.modules.xp.repository.XpAccountRepository;
import com.thesystem.modules.xp.repository.XpPolicyRepository;
import com.thesystem.modules.xp.repository.XpTransactionRepository;
import com.thesystem.modules.xp.repository.UserStreakHistoryRepository;
import com.thesystem.modules.xp.repository.UserStreakRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
@Transactional
class StreakXpIntegrationTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private GoalService goalService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private XpAccountRepository xpAccountRepository;

    @Autowired
    private XpPolicyRepository xpPolicyRepository;

    @Autowired
    private com.thesystem.modules.xp.repository.AchievementDefinitionRepository achievementDefinitionRepository;

    @Autowired
    private com.thesystem.modules.xp.repository.UserAchievementRepository userAchievementRepository;

    @Autowired
    private XpTransactionRepository xpTransactionRepository;

    @Autowired
    private UserStreakRepository userStreakRepository;

    @Autowired
    private UserStreakHistoryRepository userStreakHistoryRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private GoalRepository goalRepository;

    private UUID userId;
    private UUID goalId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        goalId = UUID.randomUUID();

        User user = new User();
        user.setEmail("test-" + userId + "@example.com");
        user.setPasswordHash("hash");
        user.setEmailVerified(true);
        userRepository.save(user);
        userId = user.getId();

        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setTimezone("UTC");
        profile.setAccountStatus("ACTIVE");
        userProfileRepository.save(profile);

        XpAccount account = new XpAccount();
        account.setId(userId);
        account.setUserId(userId);
        account.setCurrentXp(0);
        account.setCurrentLevel(1);
        account.setTotalXpEarned(0);
        account.setTotalXpSpent(0);
        account.setLifetimeXp(0);
        account.setLevelProgress(0.0);
        xpAccountRepository.save(account);

        XpPolicy taskPolicy = new XpPolicy();
        taskPolicy.setId(UUID.randomUUID());
        taskPolicy.setCode("TASK_COMPLETION");
        taskPolicy.setName("Task Completion");
        taskPolicy.setDescription("Base XP awarded for completing a task");
        taskPolicy.setPolicyType(PolicyType.TASK_COMPLETION);
        taskPolicy.setBaseXp(10);
        taskPolicy.setMultiplier(1.0);
        taskPolicy.setConditions("{\"streak_bonus\":{\"enabled\":true,\"milestones\":[3,7,14,30,60,90],\"multipliers\":[1.1,1.25,1.5,2.0,2.5,3.0]}}");
        taskPolicy.setIsActive(true);
        taskPolicy.setPriority(10);
        xpPolicyRepository.save(taskPolicy);

        XpPolicy goalPolicy = new XpPolicy();
        goalPolicy.setId(UUID.randomUUID());
        goalPolicy.setCode("GOAL_COMPLETION");
        goalPolicy.setName("Goal Completion");
        goalPolicy.setDescription("Base XP awarded for completing a goal");
        goalPolicy.setPolicyType(PolicyType.GOAL_COMPLETION);
        goalPolicy.setBaseXp(100);
        goalPolicy.setMultiplier(1.0);
        goalPolicy.setConditions("{}");
        goalPolicy.setIsActive(true);
        goalPolicy.setPriority(10);
        xpPolicyRepository.save(goalPolicy);
    }

    @Test
    void shouldApplyStreakBonusWhenTaskCompletesConsecutiveDay() {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDate yesterday = today.minusDays(1);
        LocalDate dayBefore = today.minusDays(2);

        createStreakHistory(dayBefore);
        createStreakHistory(yesterday);

        UserStreak streak = new UserStreak();
        streak.setId(userId);
        streak.setUserId(userId);
        streak.setCurrentStreak(2);
        streak.setLongestStreak(2);
        streak.setCurrentStreakStartDate(dayBefore);
        streak.setLastActivityDate(yesterday);
        userStreakRepository.save(streak);

        Task task = createTask(userId);
        taskService.completeTask(userId, task.getId());

        UserStreak updatedStreak = userStreakRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow();
        assertThat(updatedStreak.getCurrentStreak()).isEqualTo(3);
        assertThat(updatedStreak.getLastActivityDate()).isEqualTo(today);

        List<XpTransaction> transactions = xpTransactionRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
        assertThat(transactions).hasSize(1);
        XpTransaction tx = transactions.get(0);
        assertThat(tx.getAmount()).isEqualTo(11);
        assertThat(tx.getMultiplierApplied()).isEqualTo(1.1);
        assertThat(tx.getBaseAmount()).isEqualTo(10);
    }

    @Test
    void shouldApplyStreakBonusWhenGoalCompletesConsecutiveDay() {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDate yesterday = today.minusDays(1);
        LocalDate dayBefore = today.minusDays(2);

        createStreakHistory(dayBefore);
        createStreakHistory(yesterday);

        UserStreak streak = new UserStreak();
        streak.setId(userId);
        streak.setUserId(userId);
        streak.setCurrentStreak(2);
        streak.setLongestStreak(2);
        streak.setCurrentStreakStartDate(dayBefore);
        streak.setLastActivityDate(yesterday);
        userStreakRepository.save(streak);

        Goal goal = createGoal(goalId, userId);
        goalService.completeGoal(userId, goalId);

        UserStreak updatedStreak = userStreakRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow();
        assertThat(updatedStreak.getCurrentStreak()).isEqualTo(3);
        assertThat(updatedStreak.getLastActivityDate()).isEqualTo(today);

        List<XpTransaction> transactions = xpTransactionRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
        assertThat(transactions).hasSize(1);
        XpTransaction tx = transactions.get(0);
        assertThat(tx.getAmount()).isEqualTo(110);
        assertThat(tx.getMultiplierApplied()).isEqualTo(1.1);
        assertThat(tx.getBaseAmount()).isEqualTo(100);
    }

    @Test
    void shouldNotDoubleIncrementStreakForSameDayTaskAndGoal() {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDate yesterday = today.minusDays(1);
        LocalDate dayBefore = today.minusDays(2);

        createStreakHistory(dayBefore);
        createStreakHistory(yesterday);

        UserStreak streak = new UserStreak();
        streak.setId(userId);
        streak.setUserId(userId);
        streak.setCurrentStreak(2);
        streak.setLongestStreak(2);
        streak.setCurrentStreakStartDate(dayBefore);
        streak.setLastActivityDate(yesterday);
        userStreakRepository.save(streak);

        Task task = createTask(userId);
        taskService.completeTask(userId, task.getId());

        Goal goal = createGoal(goalId, userId);
        goalService.completeGoal(userId, goalId);

        UserStreak updatedStreak = userStreakRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow();
        assertThat(updatedStreak.getCurrentStreak()).isEqualTo(3);
        assertThat(updatedStreak.getLastActivityDate()).isEqualTo(today);

        List<XpTransaction> transactions = xpTransactionRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
        assertThat(transactions).hasSize(2);
        assertThat(transactions.get(0).getMultiplierApplied()).isEqualTo(1.1);
        assertThat(transactions.get(1).getMultiplierApplied()).isEqualTo(1.1);
    }

    @Test
    void shouldBeIdempotentForDuplicateTaskCompletionEvent() {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDate yesterday = today.minusDays(1);
        LocalDate dayBefore = today.minusDays(2);

        createStreakHistory(dayBefore);
        createStreakHistory(yesterday);

        UserStreak streak = new UserStreak();
        streak.setId(userId);
        streak.setUserId(userId);
        streak.setCurrentStreak(2);
        streak.setLongestStreak(2);
        streak.setCurrentStreakStartDate(dayBefore);
        streak.setLastActivityDate(yesterday);
        userStreakRepository.save(streak);

        Task task = createTask(userId);
        Instant occurredAt = java.time.Instant.now();
        TaskCompletedEvent event = new TaskCompletedEvent(task.getId(), userId, null, task.getTitle(),
                task.getExecutionType() != null ? task.getExecutionType().name() : null,
                task.getDifficulty() != null ? task.getDifficulty().name() : null,
                occurredAt);

        eventPublisher.publishEvent(event);
        eventPublisher.publishEvent(event);

        UserStreak updatedStreak = userStreakRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow();
        assertThat(updatedStreak.getCurrentStreak()).isEqualTo(3);

        long historyCount = userStreakHistoryRepository.findByUserIdAndDeletedAtIsNullOrderByActivityDateAscOccurredAtAsc(userId).stream()
                .filter(h -> h.getSourceId().equals(task.getId()))
                .count();
        assertThat(historyCount).isEqualTo(1);

        List<XpTransaction> transactions = xpTransactionRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
        assertThat(transactions).hasSize(1);
    }

    @Test
    void shouldUseZeroStreakWhenNoUserStreakExists() {
        Task task = createTask(userId);
        taskService.completeTask(userId, task.getId());

        List<XpTransaction> transactions = xpTransactionRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
        assertThat(transactions).hasSize(1);
        XpTransaction tx = transactions.get(0);
        assertThat(tx.getAmount()).isEqualTo(10);
        assertThat(tx.getMultiplierApplied()).isEqualTo(1.0);
        assertThat(tx.getBaseAmount()).isEqualTo(10);
    }

    private void createStreakHistory(LocalDate activityDate) {
        UserStreakHistory history = new UserStreakHistory();
        history.setId(UUID.randomUUID());
        history.setUserId(userId);
        history.setActivityDate(activityDate);
        history.setOccurredAt(activityDate.atStartOfDay(ZoneId.of("UTC")).toInstant());
        history.setSourceEngine("test-engine");
        history.setSourceType("TEST");
        history.setSourceId(UUID.randomUUID());
        userStreakHistoryRepository.save(history);
    }

    private Task createTask(UUID userId) {
        Task task = new Task();
        task.setUserId(userId);
        task.setTitle("Integration Test Task");
        task.setDescription("Test task for integration testing");
        task.setStatus(com.thesystem.modules.task.enums.TaskStatus.IN_PROGRESS);
        task.setPriority(com.thesystem.modules.task.enums.TaskPriority.NORMAL);
        task.setExecutionType(com.thesystem.modules.task.enums.TaskExecutionType.BOOLEAN);
        task.setVisibility(com.thesystem.modules.task.enums.TaskVisibility.PRIVATE);
        task.setIsRecurring(false);
        return taskRepository.save(task);
    }

    private Goal createGoal(UUID goalId, UUID userId) {
        Goal goal = new Goal();
        goal.setId(goalId);
        goal.setUserId(userId);
        goal.setTitle("Integration Test Goal");
        goal.setDescription("Test goal for integration testing");
        goal.setStatus(com.thesystem.modules.goal.enums.GoalStatus.ACTIVE);
        goal.setPriority(com.thesystem.modules.goal.enums.GoalPriority.NORMAL);
        goal.setDifficulty(com.thesystem.modules.goal.enums.GoalDifficulty.NORMAL);
        goal.setVisibility(com.thesystem.modules.goal.enums.GoalVisibility.PRIVATE);
        goal.setEstimatedXp(100);
        goal.setCurrentProgress(0);
        goal.setCompletionPercentage(0.0);
        return goalRepository.save(goal);
    }

    @Test
    void shouldUnlockStreakAchievementAndRecordXpRewardWhenMilestoneCrossed() {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDate yesterday = today.minusDays(1);
        LocalDate dayBefore = today.minusDays(2);

        createStreakHistory(dayBefore);
        createStreakHistory(yesterday);

        UserStreak streak = new UserStreak();
        streak.setId(userId);
        streak.setUserId(userId);
        streak.setCurrentStreak(2);
        streak.setLongestStreak(2);
        streak.setCurrentStreakStartDate(dayBefore);
        streak.setLastActivityDate(yesterday);
        userStreakRepository.save(streak);

        com.thesystem.modules.xp.entity.AchievementDefinition streak3 = new com.thesystem.modules.xp.entity.AchievementDefinition();
        streak3.setId(UUID.randomUUID());
        streak3.setCode("STREAK_3_DAY");
        streak3.setName("3-Day Streak");
        streak3.setDescription("Maintain a 3-day streak");
        streak3.setCategory(com.thesystem.modules.xp.enums.AchievementCategory.STREAK);
        streak3.setRequirementType(com.thesystem.modules.xp.enums.RequirementType.STREAK);
        streak3.setRequirementValue("{\"metric\":\"current_streak\",\"milestone\":3}");
        streak3.setXpReward(50);
        streak3.setIsHidden(false);
        streak3.setIsRepeatable(false);
        streak3.setSortOrder(1);
        achievementDefinitionRepository.save(streak3);

        Task task = createTask(userId);
        taskService.completeTask(userId, task.getId());

        UserStreak updatedStreak = userStreakRepository.findByUserIdAndDeletedAtIsNull(userId).orElseThrow();
        assertThat(updatedStreak.getCurrentStreak()).isEqualTo(3);
        assertThat(updatedStreak.getLastActivityDate()).isEqualTo(today);

        List<XpTransaction> transactions = xpTransactionRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);
        assertThat(transactions).hasSize(2);

        XpTransaction taskTx = transactions.stream()
                .filter(tx -> tx.getTransactionType() == com.thesystem.modules.xp.enums.TransactionType.TASK_COMPLETION)
                .findFirst().orElseThrow();
        assertThat(taskTx.getAmount()).isEqualTo(11);

        com.thesystem.modules.xp.entity.UserAchievement userAchievement = userAchievementRepository
                .findByUserIdAndAchievementIdAndDeletedAtIsNull(userId, streak3.getId()).orElseThrow();
        assertThat(userAchievement.getIsUnlocked()).isTrue();
        assertThat(userAchievement.getCurrentProgress()).isEqualTo(3);

        XpTransaction rewardTx = transactions.stream()
                .filter(tx -> tx.getTransactionType() == com.thesystem.modules.xp.enums.TransactionType.ACHIEVEMENT)
                .findFirst().orElseThrow();
        assertThat(rewardTx.getAmount()).isEqualTo(50);
        assertThat(rewardTx.getSourceType()).isEqualTo("ACHIEVEMENT");
    }
}
