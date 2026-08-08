package com.thesystem.modules.xp.service;

import com.thesystem.modules.xp.dto.achievement.AchievementResponse;
import com.thesystem.modules.xp.dto.level.ProgressResponse;
import com.thesystem.modules.xp.dto.policy.PolicyEvaluationResponse;
import com.thesystem.modules.xp.dto.statistics.LeaderboardEntry;
import com.thesystem.modules.xp.dto.statistics.LeaderboardResponse;
import com.thesystem.modules.xp.dto.statistics.StatisticsResponse;
import com.thesystem.modules.xp.dto.transaction.TransactionCreateRequest;
import com.thesystem.modules.xp.dto.transaction.TransactionResponse;
import com.thesystem.modules.xp.dto.xpaccount.XpAccountResponse;
import com.thesystem.modules.xp.entity.AchievementDefinition;
import com.thesystem.modules.xp.entity.UserAchievement;
import com.thesystem.modules.xp.entity.XpAccount;
import com.thesystem.modules.xp.entity.XpPolicy;
import com.thesystem.modules.xp.entity.XpTransaction;
import com.thesystem.modules.xp.enums.AchievementCategory;
import com.thesystem.modules.xp.enums.PolicyType;
import com.thesystem.modules.xp.enums.RequirementType;
import com.thesystem.modules.xp.enums.TransactionType;
import com.thesystem.modules.xp.events.AchievementUnlockedEvent;
import com.thesystem.modules.xp.events.LevelUpEvent;
import com.thesystem.modules.xp.events.XpAwardedEvent;
import com.thesystem.modules.xp.events.XpRemovedEvent;
import com.thesystem.modules.xp.exception.DuplicateTransactionException;
import com.thesystem.modules.xp.exception.InvalidTransactionException;
import com.thesystem.modules.xp.exception.LevelCalculationException;
import com.thesystem.modules.xp.exception.XpAccountNotFoundException;
import com.thesystem.modules.xp.mapper.XpMapper;
import com.thesystem.modules.xp.repository.AchievementDefinitionRepository;
import com.thesystem.modules.xp.repository.RewardHistoryRepository;
import com.thesystem.modules.xp.repository.UserAchievementRepository;
import com.thesystem.modules.xp.repository.XpAccountRepository;
import com.thesystem.modules.xp.repository.XpPolicyRepository;
import com.thesystem.modules.xp.repository.XpTransactionRepository;
import com.thesystem.modules.xp.service.impl.XpServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class XpServiceImplTest {

    @Mock
    private XpAccountRepository xpAccountRepository;

    @Mock
    private XpTransactionRepository xpTransactionRepository;

    @Mock
    private AchievementDefinitionRepository achievementDefinitionRepository;

    @Mock
    private UserAchievementRepository userAchievementRepository;

    @Mock
    private XpPolicyRepository xpPolicyRepository;

    @Mock
    private RewardHistoryRepository rewardHistoryRepository;

    @Mock
    private XpMapper xpMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private XpServiceImpl xpService;
    private UUID userId;
    private UUID accountId;
    private UUID transactionId;

    @BeforeEach
    void setUp() {
        xpService = new XpServiceImpl(
                xpAccountRepository, xpTransactionRepository,
                achievementDefinitionRepository, userAchievementRepository,
                xpPolicyRepository, rewardHistoryRepository,
                xpMapper, objectMapper, eventPublisher
        );
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        transactionId = UUID.randomUUID();

        try {
            com.fasterxml.jackson.databind.ObjectMapper realMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            org.mockito.Mockito.lenient().when(objectMapper.readValue(anyString(), any(com.fasterxml.jackson.core.type.TypeReference.class)))
                    .thenAnswer(invocation -> {
                        String json = invocation.getArgument(0);
                        com.fasterxml.jackson.core.type.TypeReference<?> typeRef = invocation.getArgument(1);
                        return realMapper.readValue(json, typeRef);
                    });
        } catch (Exception e) {
            throw new RuntimeException("Failed to setup ObjectMapper stub", e);
        }

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, null)
        );
    }

    // ========================
    // calculateLevel tests
    // ========================

    @Test
    void shouldReturnLevelOneForZeroXp() {
        int level = xpService.calculateLevel(0);
        assertThat(level).isEqualTo(1);
    }

    @Test
    void shouldReturnLevelOneAtOneHundredXp() {
        int level = xpService.calculateLevel(100);
        assertThat(level).isEqualTo(1);
    }

    @Test
    void shouldReturnLevelTwoAtTwoHundredEightyThreeXp() {
        int level = xpService.calculateLevel(283);
        assertThat(level).isEqualTo(2);
    }

    @Test
    void shouldReturnLevelThreeAtFiveHundredTwentyXp() {
        int level = xpService.calculateLevel(520);
        assertThat(level).isEqualTo(3);
    }

    @Test
    void shouldThrowForNegativeXp() {
        assertThatThrownBy(() -> xpService.calculateLevel(-1))
                .isInstanceOf(LevelCalculationException.class)
                .hasMessageContaining("XP cannot be negative");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 99, 282, 519, 799})
    void shouldReturnCorrectLevelAtBoundaries(int xp) {
        int level = xpService.calculateLevel(xp);
        assertThat(level).isGreaterThanOrEqualTo(1);
    }

    // ========================
    // calculateProgress tests
    // ========================

    @Test
    void shouldCalculateProgressForAccount() {
        XpAccount account = new XpAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setCurrentLevel(1);
        account.setLifetimeXp(0);
        account.setLevelProgress(0.0);

        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(account));

        ProgressResponse progress = xpService.calculateProgress(userId);

        assertThat(progress).isNotNull();
        assertThat(progress.currentLevel()).isEqualTo(1);
        assertThat(progress.currentXp()).isEqualTo(0);
    }

    @Test
    void shouldCalculateProgressForMidLevel() {
        XpAccount account = new XpAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setCurrentLevel(2);
        account.setLifetimeXp(191);
        account.setLevelProgress(0.0);

        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(account));

        ProgressResponse progress = xpService.calculateProgress(userId);

        assertThat(progress).isNotNull();
        assertThat(progress.currentLevel()).isEqualTo(2);
        assertThat(progress.xpRequiredForLevel()).isEqualTo(283);
    }

    @Test
    void shouldThrowWhenAccountNotFoundForProgress() {
        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> xpService.calculateProgress(userId))
                .isInstanceOf(XpAccountNotFoundException.class)
                .hasMessageContaining("XP account not found");
    }

    // ========================
    // createTransaction tests
    // ========================

    @Test
    void shouldCreateTransactionSuccessfully() {
        XpAccount account = new XpAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setCurrentXp(0);
        account.setCurrentLevel(1);
        account.setTotalXpEarned(0);
        account.setTotalXpSpent(0);
        account.setLifetimeXp(0);
        account.setLevelProgress(0.0);

        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(account));
        when(xpTransactionRepository.findBySourceEngineAndSourceIdAndSourceTypeAndDeletedAtIsNull(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(xpTransactionRepository.save(any(XpTransaction.class))).thenAnswer(invocation -> {
            XpTransaction t = invocation.getArgument(0);
            t.setId(transactionId);
            return t;
        });
        when(xpMapper.toTransactionResponse(any(XpTransaction.class))).thenAnswer(invocation -> {
            XpTransaction t = invocation.getArgument(0);
            return new TransactionResponse(
                    t.getId(), t.getUserId(), t.getTransactionType(), t.getAmount(),
                    t.getBalanceAfter(), t.getSourceEngine(), t.getSourceId(),
                    t.getSourceType(), t.getPolicyId(), t.getMultiplierApplied(),
                    t.getBaseAmount(), t.getReason(), Map.of(), Instant.now()
            );
        });
        when(xpAccountRepository.save(any(XpAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionCreateRequest request = new TransactionCreateRequest(
                TransactionType.TASK_COMPLETION, 100, "task", UUID.randomUUID(), "task", "Completed task", null
        );

        TransactionResponse response = xpService.createTransaction(request);

        assertThat(response).isNotNull();
        assertThat(response.amount()).isEqualTo(100);
        assertThat(response.balanceAfter()).isEqualTo(100);
        verify(xpTransactionRepository).save(any(XpTransaction.class));
        verify(xpAccountRepository).save(any(XpAccount.class));
    }

    @Test
    void shouldRejectDuplicateTransaction() {
        XpTransaction existing = new XpTransaction();
        existing.setId(transactionId);

        when(xpTransactionRepository.findBySourceEngineAndSourceIdAndSourceTypeAndDeletedAtIsNull(any(), any(), any()))
                .thenReturn(Optional.of(existing));

        TransactionCreateRequest request = new TransactionCreateRequest(
                TransactionType.TASK_COMPLETION, 100, "task", UUID.randomUUID(), "task", "Completed task", null
        );

        assertThatThrownBy(() -> xpService.createTransaction(request))
                .isInstanceOf(DuplicateTransactionException.class)
                .hasMessageContaining("Transaction already exists for this source");
    }

    @Test
    void shouldRejectInsufficientBalance() {
        XpAccount account = new XpAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setCurrentXp(50);
        account.setCurrentLevel(1);
        account.setLifetimeXp(50);
        account.setLevelProgress(0.0);

        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(account));
        when(xpTransactionRepository.findBySourceEngineAndSourceIdAndSourceTypeAndDeletedAtIsNull(any(), any(), any()))
                .thenReturn(Optional.empty());

        TransactionCreateRequest request = new TransactionCreateRequest(
                TransactionType.PENALTY, -100, "task", UUID.randomUUID(), "task", "Penalty", null
        );

        assertThatThrownBy(() -> xpService.createTransaction(request))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("Insufficient XP balance");
    }

    @Test
    void shouldRejectMissingRequiredFields() {
        TransactionCreateRequest request = new TransactionCreateRequest(
                TransactionType.TASK_COMPLETION, 100, null, UUID.randomUUID(), "task", "Completed task", null
        );

        assertThatThrownBy(() -> xpService.createTransaction(request))
                .isInstanceOf(InvalidTransactionException.class)
                .hasMessageContaining("sourceEngine and sourceType are required");
    }

    @Test
    void shouldCreateDefaultAccountIfNotExists() {
        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());
        when(xpTransactionRepository.findBySourceEngineAndSourceIdAndSourceTypeAndDeletedAtIsNull(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(xpTransactionRepository.save(any(XpTransaction.class))).thenAnswer(invocation -> {
            XpTransaction t = invocation.getArgument(0);
            t.setId(transactionId);
            return t;
        });
        when(xpAccountRepository.save(any(XpAccount.class))).thenAnswer(invocation -> {
            XpAccount a = invocation.getArgument(0);
            a.setId(accountId);
            return a;
        });
        when(xpMapper.toTransactionResponse(any(XpTransaction.class))).thenAnswer(invocation -> {
            XpTransaction t = invocation.getArgument(0);
            return new TransactionResponse(
                    t.getId(), t.getUserId(), t.getTransactionType(), t.getAmount(),
                    t.getBalanceAfter(), t.getSourceEngine(), t.getSourceId(),
                    t.getSourceType(), t.getPolicyId(), t.getMultiplierApplied(),
                    t.getBaseAmount(), t.getReason(), Map.of(), Instant.now()
            );
        });
        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(new XpAccount()));

        TransactionCreateRequest request = new TransactionCreateRequest(
                TransactionType.TASK_COMPLETION, 100, "task", UUID.randomUUID(), "task", "Completed task", null
        );

        TransactionResponse response = xpService.createTransaction(request);

        assertThat(response).isNotNull();
        verify(xpAccountRepository, atLeastOnce()).save(any(XpAccount.class));
    }

    @Test
    void shouldPublishLevelUpEventOnTransaction() {
        XpAccount account = new XpAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setCurrentXp(0);
        account.setCurrentLevel(1);
        account.setTotalXpEarned(0);
        account.setTotalXpSpent(0);
        account.setLifetimeXp(0);
        account.setLevelProgress(0.0);

        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(account));
        when(xpTransactionRepository.findBySourceEngineAndSourceIdAndSourceTypeAndDeletedAtIsNull(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(xpTransactionRepository.save(any(XpTransaction.class))).thenAnswer(invocation -> {
            XpTransaction t = invocation.getArgument(0);
            t.setId(transactionId);
            return t;
        });
        when(xpMapper.toTransactionResponse(any(XpTransaction.class))).thenAnswer(invocation -> {
            XpTransaction t = invocation.getArgument(0);
            return new TransactionResponse(
                    t.getId(), t.getUserId(), t.getTransactionType(), t.getAmount(),
                    t.getBalanceAfter(), t.getSourceEngine(), t.getSourceId(),
                    t.getSourceType(), t.getPolicyId(), t.getMultiplierApplied(),
                    t.getBaseAmount(), t.getReason(), Map.of(), Instant.now()
            );
        });
        when(xpAccountRepository.save(any(XpAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(account));

        TransactionCreateRequest request = new TransactionCreateRequest(
                TransactionType.TASK_COMPLETION, 283, "task", UUID.randomUUID(), "task", "Level up", null
        );

        xpService.createTransaction(request);

        verify(eventPublisher).publishEvent(any(LevelUpEvent.class));
    }

    @Test
    void shouldPublishXpAwardedEventForPositiveTransaction() {
        XpAccount account = new XpAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setCurrentXp(0);
        account.setCurrentLevel(1);
        account.setTotalXpEarned(0);
        account.setTotalXpSpent(0);
        account.setLifetimeXp(0);
        account.setLevelProgress(0.0);

        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(account));
        when(xpTransactionRepository.findBySourceEngineAndSourceIdAndSourceTypeAndDeletedAtIsNull(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(xpTransactionRepository.save(any(XpTransaction.class))).thenAnswer(invocation -> {
            XpTransaction t = invocation.getArgument(0);
            t.setId(transactionId);
            return t;
        });
        when(xpMapper.toTransactionResponse(any(XpTransaction.class))).thenAnswer(invocation -> {
            XpTransaction t = invocation.getArgument(0);
            return new TransactionResponse(
                    t.getId(), t.getUserId(), t.getTransactionType(), t.getAmount(),
                    t.getBalanceAfter(), t.getSourceEngine(), t.getSourceId(),
                    t.getSourceType(), t.getPolicyId(), t.getMultiplierApplied(),
                    t.getBaseAmount(), t.getReason(), Map.of(), Instant.now()
            );
        });
        when(xpAccountRepository.save(any(XpAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(account));

        TransactionCreateRequest request = new TransactionCreateRequest(
                TransactionType.TASK_COMPLETION, 50, "task", UUID.randomUUID(), "task", "Good job", null
        );

        xpService.createTransaction(request);

        verify(eventPublisher).publishEvent(any(XpAwardedEvent.class));
    }

    @Test
    void shouldPublishXpRemovedEventForNegativeTransaction() {
        XpAccount account = new XpAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setCurrentXp(100);
        account.setCurrentLevel(2);
        account.setTotalXpEarned(100);
        account.setTotalXpSpent(0);
        account.setLifetimeXp(100);
        account.setLevelProgress(0.0);

        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(account));
        when(xpTransactionRepository.findBySourceEngineAndSourceIdAndSourceTypeAndDeletedAtIsNull(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(xpTransactionRepository.save(any(XpTransaction.class))).thenAnswer(invocation -> {
            XpTransaction t = invocation.getArgument(0);
            t.setId(transactionId);
            return t;
        });
        when(xpMapper.toTransactionResponse(any(XpTransaction.class))).thenAnswer(invocation -> {
            XpTransaction t = invocation.getArgument(0);
            return new TransactionResponse(
                    t.getId(), t.getUserId(), t.getTransactionType(), t.getAmount(),
                    t.getBalanceAfter(), t.getSourceEngine(), t.getSourceId(),
                    t.getSourceType(), t.getPolicyId(), t.getMultiplierApplied(),
                    t.getBaseAmount(), t.getReason(), Map.of(), Instant.now()
            );
        });
        when(xpAccountRepository.save(any(XpAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(account));

        TransactionCreateRequest request = new TransactionCreateRequest(
                TransactionType.PENALTY, -50, "task", UUID.randomUUID(), "task", "Bad behavior", null
        );

        xpService.createTransaction(request);

        verify(eventPublisher).publishEvent(any(XpRemovedEvent.class));
    }

    // ========================
    // checkAchievements tests
    // ========================

    @Test
    void shouldReturnEmptyWhenNoAchievementsDefined() {
        when(achievementDefinitionRepository.findByDeletedAtIsNullOrderBySortOrderAsc()).thenReturn(List.of());

        List<AchievementResponse> unlocked = xpService.checkAchievements(userId);

        assertThat(unlocked).isEmpty();
    }

    @Test
    void shouldNotReUnlockAlreadyUnlockedAchievement() {
        AchievementDefinition definition = createAchievementDefinition(UUID.randomUUID(), "FIRST_TASK");
        UserAchievement userAchievement = createUserAchievement(UUID.randomUUID(), userId, definition.getId(), true);

        when(achievementDefinitionRepository.findByDeletedAtIsNullOrderBySortOrderAsc()).thenReturn(List.of(definition));
        when(userAchievementRepository.findByUserIdAndAchievementIdAndDeletedAtIsNull(userId, definition.getId()))
                .thenReturn(Optional.of(userAchievement));

        List<AchievementResponse> unlocked = xpService.checkAchievements(userId);

        assertThat(unlocked).isEmpty();
    }

    @Test
    void shouldUnlockAchievementWhenAlreadyAtTargetProgress() {
        AchievementDefinition definition = createAchievementDefinition(UUID.randomUUID(), "FIRST_TASK");
        UserAchievement userAchievement = createUserAchievement(UUID.randomUUID(), userId, definition.getId(), false);
        userAchievement.setCurrentProgress(100);
        userAchievement.setTargetProgress(0);

        when(achievementDefinitionRepository.findByDeletedAtIsNullOrderBySortOrderAsc()).thenReturn(List.of(definition));
        when(userAchievementRepository.findByUserIdAndAchievementIdAndDeletedAtIsNull(userId, definition.getId()))
                .thenReturn(Optional.of(userAchievement));
        when(achievementDefinitionRepository.findById(definition.getId())).thenReturn(Optional.of(definition));

        List<AchievementResponse> unlocked = xpService.checkAchievements(userId);

        assertThat(unlocked).hasSize(1);
        assertThat(unlocked.get(0).code()).isEqualTo("FIRST_TASK");
        verify(eventPublisher).publishEvent(any(AchievementUnlockedEvent.class));
    }

    @Test
    void shouldCreateUserAchievementIfNotExists() {
        AchievementDefinition definition = createAchievementDefinition(UUID.randomUUID(), "FIRST_TASK");

        when(achievementDefinitionRepository.findByDeletedAtIsNullOrderBySortOrderAsc()).thenReturn(List.of(definition));
        when(userAchievementRepository.findByUserIdAndAchievementIdAndDeletedAtIsNull(userId, definition.getId()))
                .thenReturn(Optional.empty());
        when(userAchievementRepository.save(any(UserAchievement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<AchievementResponse> unlocked = xpService.checkAchievements(userId);

        verify(userAchievementRepository).save(any(UserAchievement.class));
    }

    // ========================
    // evaluatePolicies tests
    // ========================

    @Test
    void shouldReturnBaseEvaluationWithNoPolicies() {
        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of());

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.calculatedXp()).isEqualTo(0);
        assertThat(response.multiplier()).isEqualTo(1.0);
    }

    @Test
    void shouldApplySingleActivePolicy() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_BONUS", PolicyType.TASK_COMPLETION, 50, 1.5);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(50);
        assertThat(response.multiplier()).isEqualTo(1.5);
        assertThat(response.calculatedXp()).isEqualTo(75);
    }

    @Test
    void shouldCombineMultiplePoliciesAndCapMultiplier() {
        XpPolicy policy1 = createXpPolicy(UUID.randomUUID(), "TASK_BONUS", PolicyType.TASK_COMPLETION, 50, 2.0);
        XpPolicy policy2 = createXpPolicy(UUID.randomUUID(), "STREAK_BONUS", PolicyType.STREAK_MULTIPLIER, 30, 5.0);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy1, policy2));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(80);
        assertThat(response.multiplier()).isEqualTo(10.0);
        assertThat(response.calculatedXp()).isEqualTo(800);
    }

    @Test
    void shouldIgnoreInactivePolicies() {
        XpPolicy activePolicy = createXpPolicy(UUID.randomUUID(), "ACTIVE", PolicyType.TASK_COMPLETION, 50, 1.0);
        activePolicy.setIsActive(true);
        XpPolicy inactivePolicy = createXpPolicy(UUID.randomUUID(), "INACTIVE", PolicyType.TASK_COMPLETION, 50, 2.0);
        inactivePolicy.setIsActive(false);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(activePolicy));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(50);
        assertThat(response.multiplier()).isEqualTo(1.0);
    }

    @Test
    void shouldMatchPolicyWithNoConditions() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "NO_CONDITIONS", PolicyType.TASK_COMPLETION, 50, 1.5);
        policy.setConditions(null);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(50);
        assertThat(response.multiplier()).isEqualTo(1.5);
    }

    @Test
    void shouldMatchPolicyWithEmptyConditions() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "EMPTY_CONDITIONS", PolicyType.TASK_COMPLETION, 50, 1.5);
        policy.setConditions("{}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(50);
        assertThat(response.multiplier()).isEqualTo(1.5);
    }

    @Test
    void shouldMatchPolicyWhenMinUserLevelMet() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "MIN_LEVEL", PolicyType.TASK_COMPLETION, 50, 1.5);
        policy.setConditions("{\"min_user_level\": 3}");

        XpAccount account = new XpAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setCurrentLevel(5);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));
        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(account));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(50);
    }

    @Test
    void shouldNotMatchPolicyWhenMinUserLevelNotMet() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "MIN_LEVEL", PolicyType.TASK_COMPLETION, 50, 1.5);
        policy.setConditions("{\"min_user_level\": 5}");

        XpAccount account = new XpAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setCurrentLevel(3);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));
        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(account));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(0);
        assertThat(response.multiplier()).isEqualTo(1.0);
    }

    @Test
    void shouldMatchPolicyWhenMaxUserLevelMet() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "MAX_LEVEL", PolicyType.TASK_COMPLETION, 50, 1.5);
        policy.setConditions("{\"max_user_level\": 10}");

        XpAccount account = new XpAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setCurrentLevel(5);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));
        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(account));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(50);
    }

    @Test
    void shouldNotMatchPolicyWhenMaxUserLevelExceeded() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "MAX_LEVEL", PolicyType.TASK_COMPLETION, 50, 1.5);
        policy.setConditions("{\"max_user_level\": 3}");

        XpAccount account = new XpAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setCurrentLevel(5);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));
        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(account));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(0);
        assertThat(response.multiplier()).isEqualTo(1.0);
    }

    @Test
    void shouldMatchPolicyWhenUserIsAllowed() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "ALLOWED", PolicyType.TASK_COMPLETION, 50, 1.5);
        policy.setConditions("{\"allowed_user_ids\": [\"" + userId + "\"]}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(50);
    }

    @Test
    void shouldNotMatchPolicyWhenUserIsNotAllowed() {
        UUID otherUserId = UUID.randomUUID();
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "ALLOWED", PolicyType.TASK_COMPLETION, 50, 1.5);
        policy.setConditions("{\"allowed_user_ids\": [\"" + otherUserId + "\"]}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(0);
        assertThat(response.multiplier()).isEqualTo(1.0);
    }

    @Test
    void shouldMatchPolicyWhenUserIsNotExcluded() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "EXCLUDED", PolicyType.TASK_COMPLETION, 50, 1.5);
        policy.setConditions("{\"excluded_user_ids\": [\"" + UUID.randomUUID() + "\"]}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(50);
    }

    @Test
    void shouldNotMatchPolicyWhenUserIsExcluded() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "EXCLUDED", PolicyType.TASK_COMPLETION, 50, 1.5);
        policy.setConditions("{\"excluded_user_ids\": [\"" + userId + "\"]}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(0);
        assertThat(response.multiplier()).isEqualTo(1.0);
    }

    @Test
    void shouldNotMatchPolicyWithMalformedConditions() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "MALFORMED", PolicyType.TASK_COMPLETION, 50, 1.5);
        policy.setConditions("not valid json");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(0);
        assertThat(response.multiplier()).isEqualTo(1.0);
    }

    @Test
    void shouldNotMatchPolicyWithInvalidConditionValueType() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "INVALID_TYPE", PolicyType.TASK_COMPLETION, 50, 1.5);
        policy.setConditions("{\"min_user_level\": \"five\"}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(0);
        assertThat(response.multiplier()).isEqualTo(1.0);
    }

    // ========================
    // calculatePolicyMultiplier tests
    // ========================

    @Test
    void shouldReturnBaseMultiplierWithNoPolicies() {
        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of());

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of());

        assertThat(multiplier).isEqualTo(1.0);
    }

    @Test
    void shouldApplyPriorityMultiplierForHighPriority() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"priority_multipliers\":{\"LOW\":1.0,\"NORMAL\":1.0,\"HIGH\":1.5,\"CRITICAL\":2.0}}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of("taskPriority", "HIGH"));

        assertThat(multiplier).isEqualTo(1.5);
    }

    @Test
    void shouldApplyPriorityMultiplierForNormalPriority() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"priority_multipliers\":{\"LOW\":1.0,\"NORMAL\":1.0,\"HIGH\":1.5,\"CRITICAL\":2.0}}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of("taskPriority", "NORMAL"));

        assertThat(multiplier).isEqualTo(1.0);
    }

    @Test
    void shouldNotApplyPriorityMultiplierForUnknownPriority() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"priority_multipliers\":{\"LOW\":1.0,\"NORMAL\":1.0,\"HIGH\":1.5,\"CRITICAL\":2.0}}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of("taskPriority", "UNKNOWN"));

        assertThat(multiplier).isEqualTo(1.0);
    }

    @Test
    void shouldApplyDifficultyMultiplierForHardTask() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"difficulty_multipliers\":{\"EASY\":1.0,\"NORMAL\":1.25,\"HARD\":1.5,\"EXTREME\":2.0}}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of("taskDifficulty", "HARD"));

        assertThat(multiplier).isEqualTo(1.5);
    }

    @Test
    void shouldNotApplyDifficultyMultiplierWhenTaskDifficultyMissing() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"difficulty_multipliers\":{\"EASY\":1.0,\"NORMAL\":1.25,\"HARD\":1.5,\"EXTREME\":2.0}}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of("taskPriority", "NORMAL"));

        assertThat(multiplier).isEqualTo(1.0);
    }

    @Test
    void shouldApplyStreakMultiplierForSeventhDayStreak() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"streak_bonus\":{\"enabled\":true,\"milestones\":[3,7,14,30,60,90],\"multipliers\":[1.1,1.25,1.5,2.0,2.5,3.0]}}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of("streak", 7));

        assertThat(multiplier).isEqualTo(1.25);
    }

    @Test
    void shouldNotApplyStreakMultiplierWhenStreakBelowFirstMilestone() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"streak_bonus\":{\"enabled\":true,\"milestones\":[3,7,14,30,60,90],\"multipliers\":[1.1,1.25,1.5,2.0,2.5,3.0]}}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of("streak", 1));

        assertThat(multiplier).isEqualTo(1.0);
    }

    @Test
    void shouldCombinePriorityAndDifficultyMultipliers() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"priority_multipliers\":{\"HIGH\":1.5},\"difficulty_multipliers\":{\"HARD\":1.5}}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of("taskPriority", "HIGH", "taskDifficulty", "HARD"));

        assertThat(multiplier).isEqualTo(2.25);
    }

    @Test
    void shouldCapMultiplierAtTen() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 5.0);
        policy.setConditions("{\"priority_multipliers\":{\"HIGH\":2.0}}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of("taskPriority", "HIGH"));

        assertThat(multiplier).isEqualTo(10.0);
    }

    @Test
    void shouldReturnBaseMultiplierWhenPolicyHasNoConditions() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 2.5);
        policy.setConditions(null);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of());

        assertThat(multiplier).isEqualTo(2.5);
    }

    @Test
    void shouldReturnBaseMultiplierWhenPolicyConditionsAreMalformed() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 2.5);
        policy.setConditions("not valid json");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of());

        assertThat(multiplier).isEqualTo(1.0);
    }

    @Test
    void shouldApplyGoalDifficultyMultiplier() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "GOAL_COMPLETION", PolicyType.GOAL_COMPLETION, 100, 1.0);
        policy.setConditions("{\"difficulty_multipliers\":{\"EASY\":1.0,\"NORMAL\":1.25,\"HARD\":1.5,\"EXTREME\":2.0}}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(policy));

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of("goalDifficulty", "HARD"));

        assertThat(multiplier).isEqualTo(1.5);
    }

    @Test
    void shouldCombineMatchingAndNonMatchingPolicies() {
        XpPolicy matchingPolicy = createXpPolicy(UUID.randomUUID(), "MATCHING", PolicyType.TASK_COMPLETION, 50, 1.5);
        matchingPolicy.setConditions("{\"min_user_level\": 1}");

        XpPolicy nonMatchingPolicy = createXpPolicy(UUID.randomUUID(), "NON_MATCHING", PolicyType.TASK_COMPLETION, 30, 2.0);
        nonMatchingPolicy.setConditions("{\"min_user_level\": 100}");

        XpAccount account = new XpAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setCurrentLevel(5);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNull(true)).thenReturn(List.of(matchingPolicy, nonMatchingPolicy));
        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(account));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(50);
        assertThat(response.multiplier()).isEqualTo(1.5);
    }

    // ========================
    // getStatistics tests
    // ========================

    @Test
    void shouldReturnStatisticsForExistingAccount() {
        XpAccount account = new XpAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setLifetimeXp(500);
        account.setCurrentLevel(3);
        account.setLevelProgress(50.0);

        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(account));
        when(xpTransactionRepository.sumPositiveAmountByUserIdAndCreatedAtAfter(eq(userId), any(Instant.class))).thenReturn(100);

        StatisticsResponse stats = xpService.getStatistics();

        assertThat(stats).isNotNull();
        assertThat(stats.lifetimeXp()).isEqualTo(500);
        assertThat(stats.currentLevel()).isEqualTo(3);
        assertThat(stats.levelProgress()).isEqualTo(50);
        assertThat(stats.dailyXp()).isEqualTo(100);
    }

    @Test
    void shouldReturnDefaultStatisticsWhenAccountNotFound() {
        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());
        when(xpTransactionRepository.sumPositiveAmountByUserIdAndCreatedAtAfter(eq(userId), any(Instant.class))).thenReturn(null);

        StatisticsResponse stats = xpService.getStatistics();

        assertThat(stats).isNotNull();
        assertThat(stats.lifetimeXp()).isEqualTo(0);
        assertThat(stats.currentLevel()).isEqualTo(1);
        assertThat(stats.levelProgress()).isEqualTo(0);
        assertThat(stats.dailyXp()).isEqualTo(0);
    }

    // ========================
    // getLeaderboard tests
    // ========================

    @Test
    void shouldReturnEmptyLeaderboardWhenNoAccounts() {
        when(xpAccountRepository.findByDeletedAtIsNullOrderByLifetimeXpDesc()).thenReturn(new ArrayList<>());

        LeaderboardResponse leaderboard = xpService.getLeaderboard(Pageable.ofSize(10));

        assertThat(leaderboard).isNotNull();
        assertThat(leaderboard.entries()).isEmpty();
        assertThat(leaderboard.totalPages()).isEqualTo(1);
        assertThat(leaderboard.totalElements()).isEqualTo(0);
    }

    @Test
    void shouldRankAccountsByLifetimeXp() {
        XpAccount topAccount = createXpAccount(UUID.randomUUID(), 1000, 5);
        XpAccount secondAccount = createXpAccount(UUID.randomUUID(), 500, 3);

        List<XpAccount> accounts = new ArrayList<>(List.of(topAccount, secondAccount));
        when(xpAccountRepository.findByDeletedAtIsNullOrderByLifetimeXpDesc()).thenReturn(accounts);

        LeaderboardResponse leaderboard = xpService.getLeaderboard(Pageable.ofSize(10));

        assertThat(leaderboard).isNotNull();
        assertThat(leaderboard.entries()).hasSize(2);
        assertThat(leaderboard.entries().get(0).userId()).isEqualTo(topAccount.getUserId());
        assertThat(leaderboard.entries().get(0).rank()).isEqualTo(1);
        assertThat(leaderboard.entries().get(1).userId()).isEqualTo(secondAccount.getUserId());
        assertThat(leaderboard.entries().get(1).rank()).isEqualTo(2);
    }

    @Test
    void shouldRespectPageSizeLimit() {
        XpAccount account1 = createXpAccount(UUID.randomUUID(), 1000, 5);
        XpAccount account2 = createXpAccount(UUID.randomUUID(), 900, 4);
        XpAccount account3 = createXpAccount(UUID.randomUUID(), 800, 4);

        List<XpAccount> accounts = new ArrayList<>(List.of(account1, account2, account3));
        when(xpAccountRepository.findByDeletedAtIsNullOrderByLifetimeXpDesc()).thenReturn(accounts);

        LeaderboardResponse leaderboard = xpService.getLeaderboard(Pageable.ofSize(2));

        assertThat(leaderboard).isNotNull();
        assertThat(leaderboard.entries()).hasSize(2);
    }

    @Test
    void shouldGenerateUsernameFromUserId() {
        XpAccount account = createXpAccount(UUID.randomUUID(), 1000, 5);

        List<XpAccount> accounts = new ArrayList<>(List.of(account));
        when(xpAccountRepository.findByDeletedAtIsNullOrderByLifetimeXpDesc()).thenReturn(accounts);

        LeaderboardResponse leaderboard = xpService.getLeaderboard(Pageable.ofSize(10));

        assertThat(leaderboard.entries()).hasSize(1);
        assertThat(leaderboard.entries().get(0).username()).isEqualTo("User " + account.getUserId().toString().substring(0, 8));
    }

    // ========================
    // Helper methods
    // ========================

    private AchievementDefinition createAchievementDefinition(UUID id, String code) {
        AchievementDefinition definition = new AchievementDefinition();
        definition.setId(id);
        definition.setCode(code);
        definition.setName("Test Achievement");
        definition.setDescription("Test description");
        definition.setCategory(AchievementCategory.TASK);
        definition.setIconUrl("/icons/test.png");
        definition.setRequirementType(RequirementType.COUNTER);
        definition.setRequirementValue("{\"count\": 1}");
        definition.setXpReward(50);
        definition.setIsHidden(false);
        definition.setIsRepeatable(false);
        definition.setSortOrder(1);
        definition.setCreatedAt(Instant.now());
        return definition;
    }

    private UserAchievement createUserAchievement(UUID id, UUID userId, UUID achievementId, boolean isUnlocked) {
        UserAchievement userAchievement = new UserAchievement();
        userAchievement.setId(id);
        userAchievement.setUserId(userId);
        userAchievement.setAchievementId(achievementId);
        userAchievement.setCurrentProgress(0);
        userAchievement.setTargetProgress(100);
        userAchievement.setIsUnlocked(isUnlocked);
        userAchievement.setUnlockedAt(isUnlocked ? Instant.now() : null);
        userAchievement.setProgressMetadata("{}");
        userAchievement.setCreatedAt(Instant.now());
        return userAchievement;
    }

    private XpPolicy createXpPolicy(UUID id, String code, PolicyType type, int baseXp, double multiplier) {
        XpPolicy policy = new XpPolicy();
        policy.setId(id);
        policy.setCode(code);
        policy.setName("Test Policy");
        policy.setDescription("Test policy description");
        policy.setPolicyType(type);
        policy.setBaseXp(baseXp);
        policy.setMultiplier(multiplier);
        policy.setConditions("{}");
        policy.setIsActive(true);
        policy.setPriority(1);
        policy.setCreatedAt(Instant.now());
        policy.setUpdatedAt(Instant.now());
        return policy;
    }

    private XpAccount createXpAccount(UUID id, int lifetimeXp, int currentLevel) {
        XpAccount account = new XpAccount();
        account.setId(id);
        account.setUserId(UUID.randomUUID());
        account.setCurrentXp(lifetimeXp);
        account.setCurrentLevel(currentLevel);
        account.setTotalXpEarned(lifetimeXp);
        account.setTotalXpSpent(0);
        account.setLifetimeXp(lifetimeXp);
        account.setLevelProgress(0.0);
        account.setCreatedAt(Instant.now());
        account.setUpdatedAt(Instant.now());
        return account;
    }
}
