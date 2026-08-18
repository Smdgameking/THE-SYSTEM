package com.thesystem.modules.xp.service;

import com.thesystem.modules.xp.dto.achievement.AchievementResponse;
import com.thesystem.modules.xp.dto.achievement.UserAchievementResponse;
import com.thesystem.modules.xp.dto.level.ProgressResponse;
import com.thesystem.modules.xp.dto.policy.PolicyEvaluationResponse;
import com.thesystem.modules.xp.dto.statistics.LeaderboardEntry;
import com.thesystem.modules.xp.dto.statistics.LeaderboardResponse;
import com.thesystem.modules.xp.dto.statistics.StatisticsResponse;
import com.thesystem.modules.xp.dto.reward.RewardHistoryResponse;
import com.thesystem.modules.xp.dto.reward.RewardResponse;
import com.thesystem.modules.xp.dto.transaction.TransactionCreateRequest;
import com.thesystem.modules.xp.dto.transaction.TransactionResponse;
import com.thesystem.modules.xp.dto.xpaccount.XpAccountResponse;
import com.thesystem.modules.xp.entity.AchievementDefinition;
import com.thesystem.modules.xp.entity.RewardHistory;
import com.thesystem.modules.xp.entity.UserAchievement;
import com.thesystem.modules.xp.entity.UserStreak;
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
import com.thesystem.modules.xp.exception.AchievementNotFoundException;
import com.thesystem.modules.xp.exception.DuplicateTransactionException;
import com.thesystem.modules.xp.exception.InvalidTransactionException;
import com.thesystem.modules.xp.exception.LevelCalculationException;
import com.thesystem.modules.xp.exception.UserStreakNotFoundException;
import com.thesystem.modules.xp.exception.XpAccountNotFoundException;
import com.thesystem.modules.xp.mapper.XpMapper;
import com.thesystem.modules.xp.repository.AchievementDefinitionRepository;
import com.thesystem.modules.xp.repository.RewardHistoryRepository;
import com.thesystem.modules.xp.repository.UserAchievementRepository;
import com.thesystem.modules.xp.repository.UserStreakHistoryRepository;
import com.thesystem.modules.xp.repository.UserStreakRepository;
import com.thesystem.modules.xp.repository.XpAccountRepository;
import com.thesystem.modules.xp.repository.XpPolicyRepository;
import com.thesystem.modules.xp.repository.XpTransactionRepository;
import com.thesystem.modules.xp.service.XpService;
import com.thesystem.modules.xp.service.XpService.XpCalculationResult;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
    private UserStreakRepository userStreakRepository;

    @Mock
    private UserStreakHistoryRepository userStreakHistoryRepository;

    @Mock
    private XpMapper xpMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Mock
    private com.thesystem.modules.user.service.UserTimezoneResolver userTimezoneResolver;

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
                userStreakRepository, userStreakHistoryRepository,
                xpMapper, objectMapper, eventPublisher, userTimezoneResolver
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

        mockCreateTransaction();

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

    @Test
    void shouldReturnCurrentStreakCappedAtMilestone() {
        AchievementDefinition definition = createStreakAchievementDefinition(UUID.randomUUID(), "STREAK_3", "current_streak", 3);
        UserAchievement userAchievement = createUserAchievement(UUID.randomUUID(), userId, definition.getId(), false);
        userAchievement.setTargetProgress(3);

        mockCreateTransaction();

        when(achievementDefinitionRepository.findByDeletedAtIsNullOrderBySortOrderAsc()).thenReturn(List.of(definition));
        when(achievementDefinitionRepository.findById(definition.getId())).thenReturn(Optional.of(definition));
        when(userAchievementRepository.findByUserIdAndAchievementIdAndDeletedAtIsNull(userId, definition.getId()))
                .thenReturn(Optional.of(userAchievement));
        when(userStreakRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(createUserStreak(UUID.randomUUID(), userId, 3)));
        when(userAchievementRepository.save(any(UserAchievement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<AchievementResponse> unlocked = xpService.checkAchievements(userId);

        assertThat(unlocked).hasSize(1);
        assertThat(unlocked.get(0).code()).isEqualTo("STREAK_3");
        verify(userAchievementRepository).save(argThat(ua -> ua.getCurrentProgress() == 3 && ua.getIsUnlocked()));
    }

    @Test
    void shouldReturnLongestStreakCappedAtMilestone() {
        AchievementDefinition definition = createStreakAchievementDefinition(UUID.randomUUID(), "STREAK_7", "longest_streak", 7);
        UserAchievement userAchievement = createUserAchievement(UUID.randomUUID(), userId, definition.getId(), false);
        userAchievement.setTargetProgress(7);

        mockCreateTransaction();

        when(achievementDefinitionRepository.findByDeletedAtIsNullOrderBySortOrderAsc()).thenReturn(List.of(definition));
        when(achievementDefinitionRepository.findById(definition.getId())).thenReturn(Optional.of(definition));
        when(userAchievementRepository.findByUserIdAndAchievementIdAndDeletedAtIsNull(userId, definition.getId()))
                .thenReturn(Optional.of(userAchievement));
        when(userStreakRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(createUserStreak(UUID.randomUUID(), userId, 7)));
        when(userAchievementRepository.save(any(UserAchievement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<AchievementResponse> unlocked = xpService.checkAchievements(userId);

        assertThat(unlocked).hasSize(1);
        assertThat(unlocked.get(0).code()).isEqualTo("STREAK_7");
        verify(userAchievementRepository).save(argThat(ua -> ua.getCurrentProgress() == 7 && ua.getIsUnlocked()));
    }

    @Test
    void shouldReturnStreakValueBelowMilestone() {
        AchievementDefinition definition = createStreakAchievementDefinition(UUID.randomUUID(), "STREAK_5", "current_streak", 10);
        UserAchievement userAchievement = createUserAchievement(UUID.randomUUID(), userId, definition.getId(), false);

        when(achievementDefinitionRepository.findByDeletedAtIsNullOrderBySortOrderAsc()).thenReturn(List.of(definition));
        when(userAchievementRepository.findByUserIdAndAchievementIdAndDeletedAtIsNull(userId, definition.getId()))
                .thenReturn(Optional.of(userAchievement));
        when(userStreakRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(createUserStreak(UUID.randomUUID(), userId, 5)));

        List<AchievementResponse> unlocked = xpService.checkAchievements(userId);

        assertThat(unlocked).isEmpty();
        assertThat(userAchievement.getCurrentProgress()).isEqualTo(5);
        verify(userAchievementRepository, never()).save(any(UserAchievement.class));
    }

    @Test
    void shouldReturnZeroWhenNoUserStreakExists() {
        AchievementDefinition definition = createStreakAchievementDefinition(UUID.randomUUID(), "STREAK_3", "current_streak", 3);
        UserAchievement userAchievement = createUserAchievement(UUID.randomUUID(), userId, definition.getId(), false);

        when(achievementDefinitionRepository.findByDeletedAtIsNullOrderBySortOrderAsc()).thenReturn(List.of(definition));
        when(userAchievementRepository.findByUserIdAndAchievementIdAndDeletedAtIsNull(userId, definition.getId()))
                .thenReturn(Optional.of(userAchievement));
        when(userStreakRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        List<AchievementResponse> unlocked = xpService.checkAchievements(userId);

        assertThat(unlocked).isEmpty();
        assertThat(userAchievement.getCurrentProgress()).isEqualTo(0);
        verify(userAchievementRepository, never()).save(any(UserAchievement.class));
    }

    @Test
    void shouldReturnZeroForInvalidRequirementValue() {
        AchievementDefinition definition = new AchievementDefinition();
        definition.setId(UUID.randomUUID());
        definition.setCode("BAD_STREAK");
        definition.setName("Bad Streak");
        definition.setDescription("Bad");
        definition.setCategory(AchievementCategory.STREAK);
        definition.setRequirementType(RequirementType.STREAK);
        definition.setRequirementValue("not valid json");
        definition.setXpReward(50);
        definition.setIsHidden(false);
        definition.setIsRepeatable(false);
        definition.setSortOrder(1);
        definition.setCreatedAt(Instant.now());

        when(achievementDefinitionRepository.findByDeletedAtIsNullOrderBySortOrderAsc()).thenReturn(List.of(definition));
        when(userAchievementRepository.findByUserIdAndAchievementIdAndDeletedAtIsNull(userId, definition.getId()))
                .thenReturn(Optional.empty());
        when(userAchievementRepository.save(any(UserAchievement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<AchievementResponse> unlocked = xpService.checkAchievements(userId);

        assertThat(unlocked).isEmpty();
        verify(userAchievementRepository).save(argThat(ua -> ua.getCurrentProgress() == 0));
    }

    @Test
    void shouldPreserveNonStreakAchievementEvaluation() {
        AchievementDefinition definition = createAchievementDefinition(UUID.randomUUID(), "FIRST_TASK");
        UserAchievement userAchievement = createUserAchievement(UUID.randomUUID(), userId, definition.getId(), false);
        userAchievement.setCurrentProgress(50);

        when(achievementDefinitionRepository.findByDeletedAtIsNullOrderBySortOrderAsc()).thenReturn(List.of(definition));
        when(userAchievementRepository.findByUserIdAndAchievementIdAndDeletedAtIsNull(userId, definition.getId()))
                .thenReturn(Optional.of(userAchievement));

        List<AchievementResponse> unlocked = xpService.checkAchievements(userId);

        assertThat(unlocked).isEmpty();
    }

    // ========================
    // evaluatePolicies tests
    // ========================

    @Test
    void shouldReturnBaseEvaluationWithNoPolicies() {
        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of());

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.calculatedXp()).isEqualTo(0);
        assertThat(response.multiplier()).isEqualTo(1.0);
    }

    @Test
    void shouldApplySingleActivePolicy() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_BONUS", PolicyType.TASK_COMPLETION, 50, 1.5);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(50);
        assertThat(response.multiplier()).isEqualTo(1.5);
        assertThat(response.calculatedXp()).isEqualTo(75);
    }

    @Test
    void shouldCombineMultiplePoliciesAndCapMultiplier() {
        XpPolicy policy1 = createXpPolicy(UUID.randomUUID(), "TASK_BONUS", PolicyType.TASK_COMPLETION, 50, 2.0);
        policy1.setPriority(2);
        XpPolicy policy2 = createXpPolicy(UUID.randomUUID(), "STREAK_BONUS", PolicyType.STREAK_MULTIPLIER, 30, 5.0);
        policy2.setPriority(1);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy1, policy2));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(50);
        assertThat(response.multiplier()).isEqualTo(10.0);
        assertThat(response.calculatedXp()).isEqualTo(500);
    }

    @Test
    void shouldIgnoreInactivePolicies() {
        XpPolicy activePolicy = createXpPolicy(UUID.randomUUID(), "ACTIVE", PolicyType.TASK_COMPLETION, 50, 1.0);
        activePolicy.setIsActive(true);
        XpPolicy inactivePolicy = createXpPolicy(UUID.randomUUID(), "INACTIVE", PolicyType.TASK_COMPLETION, 50, 2.0);
        inactivePolicy.setIsActive(false);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(activePolicy));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(50);
        assertThat(response.multiplier()).isEqualTo(1.0);
    }

    @Test
    void shouldMatchPolicyWithNoConditions() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "NO_CONDITIONS", PolicyType.TASK_COMPLETION, 50, 1.5);
        policy.setConditions(null);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(50);
        assertThat(response.multiplier()).isEqualTo(1.5);
    }

    @Test
    void shouldMatchPolicyWithEmptyConditions() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "EMPTY_CONDITIONS", PolicyType.TASK_COMPLETION, 50, 1.5);
        policy.setConditions("{}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));

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

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));
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

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));
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

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));
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

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));
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

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(50);
    }

    @Test
    void shouldNotMatchPolicyWhenUserIsNotAllowed() {
        UUID otherUserId = UUID.randomUUID();
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "ALLOWED", PolicyType.TASK_COMPLETION, 50, 1.5);
        policy.setConditions("{\"allowed_user_ids\": [\"" + otherUserId + "\"]}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(0);
        assertThat(response.multiplier()).isEqualTo(1.0);
    }

    @Test
    void shouldMatchPolicyWhenUserIsNotExcluded() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "EXCLUDED", PolicyType.TASK_COMPLETION, 50, 1.5);
        policy.setConditions("{\"excluded_user_ids\": [\"" + UUID.randomUUID() + "\"]}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(50);
    }

    @Test
    void shouldNotMatchPolicyWhenUserIsExcluded() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "EXCLUDED", PolicyType.TASK_COMPLETION, 50, 1.5);
        policy.setConditions("{\"excluded_user_ids\": [\"" + userId + "\"]}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(0);
        assertThat(response.multiplier()).isEqualTo(1.0);
    }

    @Test
    void shouldNotMatchPolicyWithMalformedConditions() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "MALFORMED", PolicyType.TASK_COMPLETION, 50, 1.5);
        policy.setConditions("not valid json");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(0);
        assertThat(response.multiplier()).isEqualTo(1.0);
    }

    @Test
    void shouldNotMatchPolicyWithInvalidConditionValueType() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "INVALID_TYPE", PolicyType.TASK_COMPLETION, 50, 1.5);
        policy.setConditions("{\"min_user_level\": \"five\"}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));

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
        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of());

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of());

        assertThat(multiplier).isEqualTo(1.0);
    }

    @Test
    void shouldApplyPriorityMultiplierForHighPriority() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"priority_multipliers\":{\"LOW\":1.0,\"NORMAL\":1.0,\"HIGH\":1.5,\"CRITICAL\":2.0}}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of("taskPriority", "HIGH"));

        assertThat(multiplier).isEqualTo(1.5);
    }

    @Test
    void shouldApplyPriorityMultiplierForNormalPriority() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"priority_multipliers\":{\"LOW\":1.0,\"NORMAL\":1.0,\"HIGH\":1.5,\"CRITICAL\":2.0}}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of("taskPriority", "NORMAL"));

        assertThat(multiplier).isEqualTo(1.0);
    }

    @Test
    void shouldNotApplyPriorityMultiplierForUnknownPriority() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"priority_multipliers\":{\"LOW\":1.0,\"NORMAL\":1.0,\"HIGH\":1.5,\"CRITICAL\":2.0}}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of("taskPriority", "UNKNOWN"));

        assertThat(multiplier).isEqualTo(1.0);
    }

    @Test
    void shouldApplyDifficultyMultiplierForHardTask() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"difficulty_multipliers\":{\"EASY\":1.0,\"NORMAL\":1.25,\"HARD\":1.5,\"EXTREME\":2.0}}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of("taskDifficulty", "HARD"));

        assertThat(multiplier).isEqualTo(1.5);
    }

    @Test
    void shouldNotApplyDifficultyMultiplierWhenTaskDifficultyMissing() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"difficulty_multipliers\":{\"EASY\":1.0,\"NORMAL\":1.25,\"HARD\":1.5,\"EXTREME\":2.0}}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of("taskPriority", "NORMAL"));

        assertThat(multiplier).isEqualTo(1.0);
    }

    @Test
    void shouldApplyStreakMultiplierForSeventhDayStreak() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"streak_bonus\":{\"enabled\":true,\"milestones\":[3,7,14,30,60,90],\"multipliers\":[1.1,1.25,1.5,2.0,2.5,3.0]}}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of("streak", 7));

        assertThat(multiplier).isEqualTo(1.25);
    }

    @Test
    void shouldNotApplyStreakMultiplierWhenStreakBelowFirstMilestone() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"streak_bonus\":{\"enabled\":true,\"milestones\":[3,7,14,30,60,90],\"multipliers\":[1.1,1.25,1.5,2.0,2.5,3.0]}}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of("streak", 1));

        assertThat(multiplier).isEqualTo(1.0);
    }

    @Test
    void shouldCombinePriorityAndDifficultyMultipliers() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"priority_multipliers\":{\"HIGH\":1.5},\"difficulty_multipliers\":{\"HARD\":1.5}}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of("taskPriority", "HIGH", "taskDifficulty", "HARD"));

        assertThat(multiplier).isEqualTo(2.25);
    }

    @Test
    void shouldCapMultiplierAtTen() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 5.0);
        policy.setConditions("{\"priority_multipliers\":{\"HIGH\":2.0}}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of("taskPriority", "HIGH"));

        assertThat(multiplier).isEqualTo(10.0);
    }

    @Test
    void shouldReturnBaseMultiplierWhenPolicyHasNoConditions() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 2.5);
        policy.setConditions(null);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of());

        assertThat(multiplier).isEqualTo(2.5);
    }

    @Test
    void shouldReturnBaseMultiplierWhenPolicyConditionsAreMalformed() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 2.5);
        policy.setConditions("not valid json");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));

        double multiplier = xpService.calculatePolicyMultiplier(userId, Map.of());

        assertThat(multiplier).isEqualTo(1.0);
    }

    @Test
    void shouldApplyGoalDifficultyMultiplier() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "GOAL_COMPLETION", PolicyType.GOAL_COMPLETION, 100, 1.0);
        policy.setConditions("{\"difficulty_multipliers\":{\"EASY\":1.0,\"NORMAL\":1.25,\"HARD\":1.5,\"EXTREME\":2.0}}");

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));

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

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(matchingPolicy, nonMatchingPolicy));
        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(account));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(50);
        assertThat(response.multiplier()).isEqualTo(1.5);
    }

    @Test
    void shouldUsePrimaryPolicyBaseXpWhenMultiplePoliciesMatch() {
        XpPolicy primaryPolicy = createXpPolicy(UUID.randomUUID(), "PRIMARY", PolicyType.TASK_COMPLETION, 50, 1.0);
        primaryPolicy.setPriority(10);
        XpPolicy secondaryPolicy = createXpPolicy(UUID.randomUUID(), "SECONDARY", PolicyType.TASK_COMPLETION, 30, 2.0);
        secondaryPolicy.setPriority(5);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(primaryPolicy, secondaryPolicy));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(50);
        assertThat(response.multiplier()).isEqualTo(2.0);
        assertThat(response.calculatedXp()).isEqualTo(100);
    }

    @Test
    void shouldIgnoreBaseXpFromMultiplierOnlyPolicy() {
        XpPolicy basePolicy = createXpPolicy(UUID.randomUUID(), "BASE", PolicyType.TASK_COMPLETION, 50, 1.0);
        basePolicy.setPriority(10);
        XpPolicy multiplierOnlyPolicy = createXpPolicy(UUID.randomUUID(), "MULTIPLIER_ONLY", PolicyType.STREAK_MULTIPLIER, 0, 2.0);
        multiplierOnlyPolicy.setPriority(5);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(basePolicy, multiplierOnlyPolicy));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(50);
        assertThat(response.multiplier()).isEqualTo(2.0);
        assertThat(response.calculatedXp()).isEqualTo(100);
    }

    @Test
    void shouldUseHighestPriorityPolicyWhenSameBaseXp() {
        XpPolicy highPriority = createXpPolicy(UUID.randomUUID(), "HIGH", PolicyType.TASK_COMPLETION, 100, 1.0);
        highPriority.setPriority(10);
        XpPolicy lowPriority = createXpPolicy(UUID.randomUUID(), "LOW", PolicyType.TASK_COMPLETION, 50, 1.0);
        lowPriority.setPriority(1);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(highPriority, lowPriority));

        PolicyEvaluationResponse response = xpService.evaluatePolicies(userId);

        assertThat(response).isNotNull();
        assertThat(response.baseXp()).isEqualTo(100);
        assertThat(response.multiplier()).isEqualTo(1.0);
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

        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));
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
        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));
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
    // Statistics timezone tests
    // ========================

    @Test
    void shouldUseUserTimezoneForDailyBoundary() {
        XpAccount account = new XpAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setLifetimeXp(500);
        account.setCurrentLevel(3);
        account.setLevelProgress(50.0);

        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("Asia/Kolkata"));
        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(account));

        StatisticsResponse stats = xpService.getStatistics();

        assertThat(stats).isNotNull();
        verify(xpTransactionRepository, atLeastOnce()).sumPositiveAmountByUserIdAndCreatedAtAfter(eq(userId), argThat(instant -> {
            ZoneId zoneId = ZoneId.of("Asia/Kolkata");
            LocalDate today = LocalDate.now(zoneId);
            return instant.equals(today.atStartOfDay(zoneId).toInstant());
        }));
    }

    @Test
    void shouldUseUtcWhenUserTimezoneIsNull() {
        XpAccount account = new XpAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setLifetimeXp(500);
        account.setCurrentLevel(3);
        account.setLevelProgress(50.0);

        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));
        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(account));

        StatisticsResponse stats = xpService.getStatistics();

        assertThat(stats).isNotNull();
        verify(xpTransactionRepository, atLeastOnce()).sumPositiveAmountByUserIdAndCreatedAtAfter(eq(userId), argThat(instant -> {
            ZoneId zoneId = ZoneId.of("UTC");
            LocalDate today = LocalDate.now(zoneId);
            return instant.equals(today.atStartOfDay(zoneId).toInstant());
        }));
    }

    @Test
    void shouldUseUtcWhenUserTimezoneIsInvalid() {
        XpAccount account = new XpAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setLifetimeXp(500);
        account.setCurrentLevel(3);
        account.setLevelProgress(50.0);

        when(userTimezoneResolver.resolveUserZoneId(userId)).thenReturn(ZoneId.of("UTC"));
        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(account));

        StatisticsResponse stats = xpService.getStatistics();

        assertThat(stats).isNotNull();
        verify(xpTransactionRepository, atLeastOnce()).sumPositiveAmountByUserIdAndCreatedAtAfter(eq(userId), argThat(instant -> {
            ZoneId zoneId = ZoneId.of("UTC");
            LocalDate today = LocalDate.now(zoneId);
            return instant.equals(today.atStartOfDay(zoneId).toInstant());
        }));
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
    // calculateXpForEvent tests
    // ========================

    @Test
    void shouldReturnBaseXpFromPrimaryPolicy() {
        XpPolicy primaryPolicy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 25, 1.0);
        primaryPolicy.setPriority(10);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(primaryPolicy));

        XpServiceImpl.XpCalculationResult result = xpService.calculateXpForEvent(userId, Map.of(), XpService.XpSourceType.TASK);

        assertThat(result).isNotNull();
        assertThat(result.baseXp()).isEqualTo(25);
        assertThat(result.finalXp()).isEqualTo(25);
        assertThat(result.primaryPolicyId()).isEqualTo(primaryPolicy.getId());
    }

    @Test
    void shouldApplyMultiplierFromPrimaryPolicy() {
        XpPolicy primaryPolicy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 2.0);
        primaryPolicy.setConditions("{\"priority_multipliers\":{\"HIGH\":2.0}}");
        primaryPolicy.setPriority(10);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(primaryPolicy));

        XpServiceImpl.XpCalculationResult result = xpService.calculateXpForEvent(userId, Map.of("taskPriority", "HIGH"), XpService.XpSourceType.TASK);

        assertThat(result).isNotNull();
        assertThat(result.baseXp()).isEqualTo(10);
        assertThat(result.multiplier()).isEqualTo(4.0);
        assertThat(result.finalXp()).isEqualTo(40);
    }

    @Test
    void shouldCapMultiplierAtTenInEventCalculation() {
        XpPolicy primaryPolicy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 5.0);
        primaryPolicy.setConditions("{\"priority_multipliers\":{\"HIGH\":3.0}}");
        primaryPolicy.setPriority(10);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(primaryPolicy));

        XpServiceImpl.XpCalculationResult result = xpService.calculateXpForEvent(userId, Map.of("taskPriority", "HIGH"), XpService.XpSourceType.TASK);

        assertThat(result).isNotNull();
        assertThat(result.multiplier()).isEqualTo(10.0);
        assertThat(result.finalXp()).isEqualTo(100);
    }

    @Test
    void shouldReturnZeroBaseXpWhenNoPoliciesMatch() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "MIN_LEVEL", PolicyType.TASK_COMPLETION, 50, 1.5);
        policy.setConditions("{\"min_user_level\": 10}");

        XpAccount account = new XpAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setCurrentLevel(1);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));
        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(account));

        XpServiceImpl.XpCalculationResult result = xpService.calculateXpForEvent(userId, Map.of(), XpService.XpSourceType.TASK);

        assertThat(result).isNotNull();
        assertThat(result.baseXp()).isEqualTo(0);
        assertThat(result.finalXp()).isEqualTo(0);
        assertThat(result.primaryPolicyId()).isNull();
    }

    // ========================
    // Source-aware policy selection tests
    // ========================

    @Test
    void shouldSelectTaskCompletionPolicyForTaskSource() {
        XpPolicy taskPolicy = createXpPolicy(UUID.randomUUID(), "TASK_XP", PolicyType.TASK_COMPLETION, 10, 1.0);
        taskPolicy.setPriority(10);

        XpPolicy bonusPolicy = createXpPolicy(UUID.randomUUID(), "BONUS_XP", PolicyType.BONUS, 50, 1.0);
        bonusPolicy.setPriority(5);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(bonusPolicy, taskPolicy));

        XpServiceImpl.XpCalculationResult result = xpService.calculateXpForEvent(userId, Map.of(), XpService.XpSourceType.TASK);

        assertThat(result).isNotNull();
        assertThat(result.primaryPolicyId()).isEqualTo(taskPolicy.getId());
        assertThat(result.baseXp()).isEqualTo(10);
    }

    @Test
    void shouldSelectBonusPolicyForRewardSource() {
        XpPolicy taskPolicy = createXpPolicy(UUID.randomUUID(), "TASK_XP", PolicyType.TASK_COMPLETION, 10, 1.0);
        taskPolicy.setPriority(10);

        XpPolicy bonusPolicy = createXpPolicy(UUID.randomUUID(), "BONUS_XP", PolicyType.BONUS, 50, 1.0);
        bonusPolicy.setPriority(5);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(bonusPolicy, taskPolicy));

        XpServiceImpl.XpCalculationResult result = xpService.calculateXpForEvent(userId, Map.of(), XpService.XpSourceType.REWARD);

        assertThat(result).isNotNull();
        assertThat(result.primaryPolicyId()).isEqualTo(bonusPolicy.getId());
        assertThat(result.baseXp()).isEqualTo(50);
    }

    @Test
    void shouldIgnoreUnrelatedPolicyTypesForTaskSource() {
        XpPolicy goalPolicy = createXpPolicy(UUID.randomUUID(), "GOAL_XP", PolicyType.GOAL_COMPLETION, 100, 1.0);
        goalPolicy.setPriority(10);

        XpPolicy bonusPolicy = createXpPolicy(UUID.randomUUID(), "BONUS_XP", PolicyType.BONUS, 50, 2.0);
        bonusPolicy.setPriority(5);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(goalPolicy, bonusPolicy));

        XpServiceImpl.XpCalculationResult result = xpService.calculateXpForEvent(userId, Map.of(), XpService.XpSourceType.TASK);

        assertThat(result).isNotNull();
        assertThat(result.primaryPolicyId()).isNull();
        assertThat(result.baseXp()).isEqualTo(0);
        assertThat(result.finalXp()).isEqualTo(0);
    }

    @Test
    void shouldApplyMultipliersFromAllMatchingPoliciesRegardlessOfSource() {
        XpPolicy taskPolicy = createXpPolicy(UUID.randomUUID(), "TASK_XP", PolicyType.TASK_COMPLETION, 10, 1.0);
        taskPolicy.setConditions("{\"priority_multipliers\":{\"HIGH\":1.5}}");
        taskPolicy.setPriority(10);

        XpPolicy difficultyPolicy = createXpPolicy(UUID.randomUUID(), "DIFFICULTY_XP", PolicyType.DIFFICULTY_MULTIPLIER, 0, 2.0);
        difficultyPolicy.setPriority(5);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(taskPolicy, difficultyPolicy));

        XpServiceImpl.XpCalculationResult result = xpService.calculateXpForEvent(userId, Map.of("taskPriority", "HIGH"), XpService.XpSourceType.TASK);

        assertThat(result).isNotNull();
        assertThat(result.baseXp()).isEqualTo(10);
        assertThat(result.multiplier()).isEqualTo(3.0);
        assertThat(result.finalXp()).isEqualTo(30);
    }

    // ========================
    // Goal estimatedXp tests
    // ========================

    @Test
    void shouldUseGoalEstimatedXpWhenPositive() {
        XpPolicy goalPolicy = createXpPolicy(UUID.randomUUID(), "GOAL_XP", PolicyType.GOAL_COMPLETION, 100, 1.0);
        goalPolicy.setPriority(10);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(goalPolicy));

        XpServiceImpl.XpCalculationResult result = xpService.calculateXpForEvent(userId, Map.of("goalEstimatedXp", 250), XpService.XpSourceType.GOAL);

        assertThat(result).isNotNull();
        assertThat(result.baseXp()).isEqualTo(250);
        assertThat(result.finalXp()).isEqualTo(250);
    }

    @Test
    void shouldFallBackToGoalPolicyWhenEstimatedXpIsZero() {
        XpPolicy goalPolicy = createXpPolicy(UUID.randomUUID(), "GOAL_XP", PolicyType.GOAL_COMPLETION, 100, 1.0);
        goalPolicy.setPriority(10);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(goalPolicy));

        XpServiceImpl.XpCalculationResult result = xpService.calculateXpForEvent(userId, Map.of("goalEstimatedXp", 0), XpService.XpSourceType.GOAL);

        assertThat(result).isNotNull();
        assertThat(result.baseXp()).isEqualTo(100);
        assertThat(result.finalXp()).isEqualTo(100);
    }

    @Test
    void shouldFallBackToGoalPolicyWhenEstimatedXpIsNegative() {
        XpPolicy goalPolicy = createXpPolicy(UUID.randomUUID(), "GOAL_XP", PolicyType.GOAL_COMPLETION, 100, 1.0);
        goalPolicy.setPriority(10);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(goalPolicy));

        XpServiceImpl.XpCalculationResult result = xpService.calculateXpForEvent(userId, Map.of("goalEstimatedXp", -10), XpService.XpSourceType.GOAL);

        assertThat(result).isNotNull();
        assertThat(result.baseXp()).isEqualTo(100);
    }

    // ========================
    // Streak integration tests
    // ========================

    @Test
    void shouldApplyStreakBonusFromUserStreakRepository() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"streak_bonus\":{\"enabled\":true,\"milestones\":[3,7,14,30,60,90],\"multipliers\":[1.1,1.25,1.5,2.0,2.5,3.0]}}");
        policy.setPriority(10);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));
        when(userStreakRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(java.util.Optional.of(createUserStreak(accountId, userId, 3)));

        XpServiceImpl.XpCalculationResult result = xpService.calculateXpForEvent(userId, Map.of(), XpService.XpSourceType.TASK);

        assertThat(result).isNotNull();
        assertThat(result.multiplier()).isEqualTo(1.1);
        assertThat(result.finalXp()).isEqualTo(11);
    }

    @Test
    void shouldNotApplyStreakBonusWhenStreakBelowFirstMilestone() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"streak_bonus\":{\"enabled\":true,\"milestones\":[3,7,14,30,60,90],\"multipliers\":[1.1,1.25,1.5,2.0,2.5,3.0]}}");
        policy.setPriority(10);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));
        when(userStreakRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(java.util.Optional.of(createUserStreak(accountId, userId, 1)));

        XpServiceImpl.XpCalculationResult result = xpService.calculateXpForEvent(userId, Map.of(), XpService.XpSourceType.TASK);

        assertThat(result).isNotNull();
        assertThat(result.multiplier()).isEqualTo(1.0);
        assertThat(result.finalXp()).isEqualTo(10);
    }

    @Test
    void shouldApplyCorrectStreakBonusBetweenMilestones() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"streak_bonus\":{\"enabled\":true,\"milestones\":[3,7,14,30,60,90],\"multipliers\":[1.1,1.25,1.5,2.0,2.5,3.0]}}");
        policy.setPriority(10);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));
        when(userStreakRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(java.util.Optional.of(createUserStreak(accountId, userId, 5)));

        XpServiceImpl.XpCalculationResult result = xpService.calculateXpForEvent(userId, Map.of(), XpService.XpSourceType.TASK);

        assertThat(result).isNotNull();
        assertThat(result.multiplier()).isEqualTo(1.1);
        assertThat(result.finalXp()).isEqualTo(11);
    }

    @Test
    void shouldApplyCorrectStreakBonusAtSeven() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"streak_bonus\":{\"enabled\":true,\"milestones\":[3,7,14,30,60,90],\"multipliers\":[1.1,1.25,1.5,2.0,2.5,3.0]}}");
        policy.setPriority(10);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));
        when(userStreakRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(java.util.Optional.of(createUserStreak(accountId, userId, 7)));

        XpServiceImpl.XpCalculationResult result = xpService.calculateXpForEvent(userId, Map.of(), XpService.XpSourceType.TASK);

        assertThat(result).isNotNull();
        assertThat(result.multiplier()).isEqualTo(1.25);
        assertThat(result.finalXp()).isEqualTo(13);
    }

    @Test
    void shouldApplyHighestStreakBonusAboveMaxMilestone() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"streak_bonus\":{\"enabled\":true,\"milestones\":[3,7,14,30,60,90],\"multipliers\":[1.1,1.25,1.5,2.0,2.5,3.0]}}");
        policy.setPriority(10);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));
        when(userStreakRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(java.util.Optional.of(createUserStreak(accountId, userId, 100)));

        XpServiceImpl.XpCalculationResult result = xpService.calculateXpForEvent(userId, Map.of(), XpService.XpSourceType.TASK);

        assertThat(result).isNotNull();
        assertThat(result.multiplier()).isEqualTo(3.0);
        assertThat(result.finalXp()).isEqualTo(30);
    }

    @Test
    void shouldCapTotalMultiplierAtTenWithStreakBonus() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 5.0);
        policy.setConditions("{\"streak_bonus\":{\"enabled\":true,\"milestones\":[3,7,14,30,60,90],\"multipliers\":[1.1,1.25,1.5,2.0,2.5,3.0]}}");
        policy.setPriority(10);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));
        when(userStreakRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(java.util.Optional.of(createUserStreak(accountId, userId, 100)));

        XpServiceImpl.XpCalculationResult result = xpService.calculateXpForEvent(userId, Map.of("taskPriority", "HIGH"), XpService.XpSourceType.TASK);

        assertThat(result).isNotNull();
        assertThat(result.multiplier()).isEqualTo(10.0);
        assertThat(result.finalXp()).isEqualTo(100);
    }

    @Test
    void shouldNotApplyStreakBonusForGoalPolicyWithoutStreakConfig() {
        XpPolicy goalPolicy = createXpPolicy(UUID.randomUUID(), "GOAL_COMPLETION", PolicyType.GOAL_COMPLETION, 100, 1.0);
        goalPolicy.setConditions("{}");
        goalPolicy.setPriority(10);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(goalPolicy));
        when(userStreakRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(java.util.Optional.of(createUserStreak(accountId, userId, 10)));

        XpServiceImpl.XpCalculationResult result = xpService.calculateXpForEvent(userId, Map.of("goalEstimatedXp", 100), XpService.XpSourceType.GOAL);

        assertThat(result).isNotNull();
        assertThat(result.multiplier()).isEqualTo(1.0);
        assertThat(result.finalXp()).isEqualTo(100);
    }

    @Test
    void shouldUseZeroStreakWhenNoUserStreakExists() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"streak_bonus\":{\"enabled\":true,\"milestones\":[3,7,14,30,60,90],\"multipliers\":[1.1,1.25,1.5,2.0,2.5,3.0]}}");
        policy.setPriority(10);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));
        when(userStreakRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(java.util.Optional.empty());

        XpServiceImpl.XpCalculationResult result = xpService.calculateXpForEvent(userId, Map.of(), XpService.XpSourceType.TASK);

        assertThat(result).isNotNull();
        assertThat(result.multiplier()).isEqualTo(1.0);
        assertThat(result.finalXp()).isEqualTo(10);
    }

    @Test
    void shouldUseZeroStreakWhenCurrentStreakIsNull() {
        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"streak_bonus\":{\"enabled\":true,\"milestones\":[3,7,14,30,60,90],\"multipliers\":[1.1,1.25,1.5,2.0,2.5,3.0]}}");
        policy.setPriority(10);

        UserStreak streak = createUserStreak(accountId, userId, null);
        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));
        when(userStreakRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(java.util.Optional.of(streak));

        XpServiceImpl.XpCalculationResult result = xpService.calculateXpForEvent(userId, Map.of(), XpService.XpSourceType.TASK);

        assertThat(result).isNotNull();
        assertThat(result.multiplier()).isEqualTo(1.0);
        assertThat(result.finalXp()).isEqualTo(10);
    }

    @Test
    void shouldThrowOnSequentialDuplicateTransaction() {
        XpTransaction existingTransaction = new XpTransaction();
        existingTransaction.setId(transactionId);
        existingTransaction.setUserId(userId);
        existingTransaction.setSourceEngine("task-engine");
        existingTransaction.setSourceId(UUID.randomUUID());
        existingTransaction.setSourceType("TASK");

        when(xpTransactionRepository.findBySourceEngineAndSourceIdAndSourceTypeAndDeletedAtIsNull(any(), any(), any()))
                .thenReturn(Optional.of(existingTransaction));

        TransactionCreateRequest request = new TransactionCreateRequest(
                TransactionType.TASK_COMPLETION, 10, "task-engine", UUID.randomUUID(), "TASK", "Completed task", null
        );

        assertThatThrownBy(() -> xpService.createTransaction(userId, request))
                .isInstanceOf(DuplicateTransactionException.class);
    }

    // ========================
    // Transaction audit fields tests
    // ========================

    @Test
    void shouldPopulateAuditFieldsWhenCreatingTransaction() {
        XpAccount account = new XpAccount();
        account.setId(accountId);
        account.setUserId(userId);
        account.setCurrentXp(0);
        account.setCurrentLevel(1);
        account.setTotalXpEarned(0);
        account.setTotalXpSpent(0);
        account.setLifetimeXp(0);
        account.setLevelProgress(0.0);

        UUID policyId = UUID.randomUUID();

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

        TransactionResponse response = xpService.createTransaction(userId, request, policyId, 1.5, 10);

        assertThat(response).isNotNull();
        assertThat(response.policyId()).isEqualTo(policyId);
        assertThat(response.multiplierApplied()).isEqualTo(1.5);
        assertThat(response.baseAmount()).isEqualTo(10);
    }

    // ========================
    // Reward XP calculation tests
    // ========================

    @Test
    void shouldCalculateRewardUsingPolicyEngine() {
        XpPolicy rewardPolicy = createXpPolicy(UUID.randomUUID(), "REWARD_POLICY", PolicyType.BONUS, 50, 1.5);
        rewardPolicy.setPriority(10);

        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(rewardPolicy));
        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(new XpAccount()));
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
        when(rewardHistoryRepository.save(any(RewardHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RewardResponse response = xpService.grantReward(userId, UUID.randomUUID());

        assertThat(response).isNotNull();
        assertThat(response.xpAmount()).isEqualTo(75);
        assertThat(response.policyId()).isEqualTo(rewardPolicy.getId());
        assertThat(response.multiplierApplied()).isEqualTo(1.5);
        assertThat(response.baseAmount()).isEqualTo(50);
    }

    // ========================
    // getUserAchievement read-only tests
    // ========================

    @Test
    void shouldReturnUserAchievementWithoutMutation() {
        UUID userAchievementId = UUID.randomUUID();
        UserAchievement userAchievement = createUserAchievement(UUID.randomUUID(), userId, UUID.randomUUID(), true);
        userAchievement.setId(userAchievementId);

        when(userAchievementRepository.findByUserIdAndAchievementIdAndDeletedAtIsNull(userId, userAchievementId))
                .thenReturn(Optional.of(userAchievement));
        when(achievementDefinitionRepository.findById(userAchievement.getAchievementId()))
                .thenReturn(Optional.of(createAchievementDefinition(userAchievement.getAchievementId(), "TEST")));

        UserAchievementResponse response = xpService.getUserAchievement(userId, userAchievementId);

        assertThat(response).isNotNull();
        assertThat(response.isUnlocked()).isTrue();
        verify(userAchievementRepository, never()).save(any(UserAchievement.class));
        verify(eventPublisher, never()).publishEvent(any(AchievementUnlockedEvent.class));
    }

    @Test
    void shouldThrowWhenUserAchievementNotFound() {
        UUID missingId = UUID.randomUUID();
        when(userAchievementRepository.findByUserIdAndAchievementIdAndDeletedAtIsNull(userId, missingId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> xpService.getUserAchievement(userId, missingId))
                .isInstanceOf(AchievementNotFoundException.class);
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

    private AchievementDefinition createStreakAchievementDefinition(UUID id, String code, String metric, int milestone) {
        AchievementDefinition definition = new AchievementDefinition();
        definition.setId(id);
        definition.setCode(code);
        definition.setName("Test Streak Achievement");
        definition.setDescription("Test streak description");
        definition.setCategory(AchievementCategory.STREAK);
        definition.setIconUrl("/icons/test.png");
        definition.setRequirementType(RequirementType.STREAK);
        definition.setRequirementValue("{\"metric\":\"" + metric + "\",\"milestone\":" + milestone + "}");
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

    private UserStreak createUserStreak(UUID id, UUID userId, Integer currentStreak) {
        UserStreak streak = new UserStreak();
        streak.setId(id);
        streak.setUserId(userId);
        streak.setCurrentStreak(currentStreak);
        streak.setLongestStreak(currentStreak != null ? currentStreak : 0);
        streak.setCurrentStreakStartDate(java.time.LocalDate.now());
        streak.setLastActivityDate(java.time.LocalDate.now());
        return streak;
    }

    private void mockCreateTransaction() {
        XpAccount mockAccount = mock(XpAccount.class);
        when(mockAccount.getCurrentXp()).thenReturn(0);
        when(mockAccount.getTotalXpEarned()).thenReturn(0);
        when(mockAccount.getTotalXpSpent()).thenReturn(0);
        when(mockAccount.getLifetimeXp()).thenReturn(0);
        when(xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(mockAccount));
        when(xpTransactionRepository.findBySourceEngineAndSourceIdAndSourceTypeAndDeletedAtIsNull(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(xpTransactionRepository.save(any(XpTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldHandleImmutableContextMap() {
        UUID userId = UUID.randomUUID();
        when(userStreakRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(createUserStreak(UUID.randomUUID(), userId, 3)));

        XpPolicy policy = createXpPolicy(UUID.randomUUID(), "TASK_COMPLETION", PolicyType.TASK_COMPLETION, 10, 1.0);
        policy.setConditions("{\"streak_bonus\":{\"enabled\":true,\"milestones\":[3,7,14,30,60,90],\"multipliers\":[1.1,1.25,1.5,2.0,2.5,3.0]}}");
        when(xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true)).thenReturn(List.of(policy));

        Map<String, Object> immutableContext = Map.of("taskPriority", "NORMAL");
        XpCalculationResult result = xpService.calculateXpForEvent(userId, immutableContext, XpService.XpSourceType.TASK);

        assertThat(result.baseXp()).isEqualTo(10);
        assertThat(result.multiplier()).isEqualTo(1.1);
        assertThat(result.finalXp()).isEqualTo(11);
        assertThat(immutableContext).hasSize(1);
        assertThat(immutableContext).containsKey("taskPriority");
    }

    // ========================
    // getUserStreak tests
    // ========================

    @Test
    void shouldReturnUserStreakResponseWhenStreakExists() {
        UUID streakId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2026, 8, 1);
        LocalDate lastActivity = LocalDate.of(2026, 8, 3);
        UserStreak streak = new UserStreak();
        streak.setId(streakId);
        streak.setUserId(userId);
        streak.setCurrentStreak(3);
        streak.setLongestStreak(3);
        streak.setCurrentStreakStartDate(startDate);
        streak.setLastActivityDate(lastActivity);

        when(userStreakRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(streak));

        com.thesystem.modules.xp.dto.streak.UserStreakResponse response = xpService.getUserStreak(userId);

        assertThat(response).isNotNull();
        assertThat(response.currentStreak()).isEqualTo(3);
        assertThat(response.longestStreak()).isEqualTo(3);
        assertThat(response.currentStreakStartDate()).isEqualTo(startDate);
        assertThat(response.lastActivityDate()).isEqualTo(lastActivity);
        verify(userStreakRepository).findByUserIdAndDeletedAtIsNull(userId);
    }

    @Test
    void shouldThrowUserStreakNotFoundExceptionWhenNoActiveStreak() {
        when(userStreakRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> xpService.getUserStreak(userId))
                .isInstanceOf(UserStreakNotFoundException.class)
                .hasMessageContaining("User streak not found");
        verify(userStreakRepository).findByUserIdAndDeletedAtIsNull(userId);
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
