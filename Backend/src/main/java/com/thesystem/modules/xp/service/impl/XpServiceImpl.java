package com.thesystem.modules.xp.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.thesystem.modules.xp.entity.AchievementDefinition;
import com.thesystem.modules.xp.entity.RewardHistory;
import com.thesystem.modules.xp.entity.UserAchievement;
import com.thesystem.modules.xp.entity.XpAccount;
import com.thesystem.modules.xp.entity.XpPolicy;
import com.thesystem.modules.xp.entity.XpTransaction;
import com.thesystem.modules.xp.enums.AchievementCategory;
import com.thesystem.modules.xp.enums.PolicyType;
import com.thesystem.modules.xp.enums.TransactionType;
import com.thesystem.modules.xp.events.AchievementProgressUpdatedEvent;
import com.thesystem.modules.xp.events.AchievementUnlockedEvent;
import com.thesystem.modules.xp.events.LevelUpEvent;
import com.thesystem.modules.xp.events.PolicyChangedEvent;
import com.thesystem.modules.xp.events.RewardGrantedEvent;
import com.thesystem.modules.xp.events.XpAdjustedEvent;
import com.thesystem.modules.xp.events.XpAwardedEvent;
import com.thesystem.modules.xp.events.XpRemovedEvent;
import com.thesystem.modules.xp.exception.AchievementNotFoundException;
import com.thesystem.modules.xp.exception.DuplicateTransactionException;
import com.thesystem.modules.xp.exception.InvalidRewardException;
import com.thesystem.modules.xp.exception.InvalidTransactionException;
import com.thesystem.modules.xp.exception.LevelCalculationException;
import com.thesystem.modules.xp.exception.PolicyNotFoundException;
import com.thesystem.modules.xp.exception.TransactionNotFoundException;
import com.thesystem.modules.xp.exception.XpAccountNotFoundException;
import com.thesystem.modules.xp.exception.XpException;
import com.thesystem.modules.xp.mapper.XpMapper;
import com.thesystem.modules.xp.repository.AchievementDefinitionRepository;
import com.thesystem.modules.xp.repository.RewardHistoryRepository;
import com.thesystem.modules.xp.repository.UserAchievementRepository;
import com.thesystem.modules.xp.repository.XpAccountRepository;
import com.thesystem.modules.xp.repository.XpPolicyRepository;
import com.thesystem.modules.xp.repository.XpTransactionRepository;
import com.thesystem.modules.xp.service.XpService;
import com.thesystem.modules.user.service.UserTimezoneResolver;
import com.thesystem.security.util.SecurityUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class XpServiceImpl implements XpService {

    private final XpAccountRepository xpAccountRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final AchievementDefinitionRepository achievementDefinitionRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final XpPolicyRepository xpPolicyRepository;
    private final RewardHistoryRepository rewardHistoryRepository;
    private final XpMapper xpMapper;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final UserTimezoneResolver userTimezoneResolver;

    public XpServiceImpl(
            XpAccountRepository xpAccountRepository,
            XpTransactionRepository xpTransactionRepository,
            AchievementDefinitionRepository achievementDefinitionRepository,
            UserAchievementRepository userAchievementRepository,
            XpPolicyRepository xpPolicyRepository,
            RewardHistoryRepository rewardHistoryRepository,
            XpMapper xpMapper,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher,
            UserTimezoneResolver userTimezoneResolver) {
        this.xpAccountRepository = xpAccountRepository;
        this.xpTransactionRepository = xpTransactionRepository;
        this.achievementDefinitionRepository = achievementDefinitionRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.xpPolicyRepository = xpPolicyRepository;
        this.rewardHistoryRepository = rewardHistoryRepository;
        this.xpMapper = xpMapper;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.userTimezoneResolver = userTimezoneResolver;
    }

    @Override
    @Transactional(readOnly = true)
    public XpAccountResponse getAccount(UUID userId) {
        XpAccount account = xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new XpAccountNotFoundException("XP account not found"));
        return xpMapper.toXpAccountResponse(account);
    }

    @Override
    @Transactional
    public XpAccountResponse createAccount(XpAccountCreateRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        if (xpAccountRepository.existsByUserIdAndDeletedAtIsNull(userId)) {
            throw new InvalidTransactionException("XP account already exists");
        }
        XpAccount account = new XpAccount();
        account.setUserId(userId);
        account.setCurrentXp(0);
        account.setCurrentLevel(1);
        account.setTotalXpEarned(0);
        account.setTotalXpSpent(0);
        account.setLifetimeXp(0);
        account.setLevelProgress(0.0);
        XpAccount saved = xpAccountRepository.save(account);
        return xpMapper.toXpAccountResponse(saved);
    }

    @Override
    @Transactional
    public TransactionResponse createTransaction(TransactionCreateRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();
        return createTransactionInternal(userId, request, null, null, null);
    }

    @Override
    @Transactional
    public TransactionResponse createTransaction(UUID userId, TransactionCreateRequest request) {
        return createTransactionInternal(userId, request, null, null, null);
    }

    @Override
    @Transactional
    public TransactionResponse createTransaction(UUID userId, TransactionCreateRequest request, UUID policyId, Double multiplierApplied, Integer baseAmount) {
        return createTransactionInternal(userId, request, policyId, multiplierApplied, baseAmount);
    }

    private TransactionResponse createTransactionInternal(UUID userId, TransactionCreateRequest request,
                                                          UUID policyId, Double multiplierApplied, Integer baseAmount) {
        String sourceEngine = request.sourceEngine();
        UUID sourceId = request.sourceId();
        String sourceType = request.sourceType();

        if (sourceEngine == null || sourceType == null) {
            throw new InvalidTransactionException("sourceEngine and sourceType are required");
        }

        Optional<XpTransaction> existing = xpTransactionRepository
                .findBySourceEngineAndSourceIdAndSourceTypeAndDeletedAtIsNull(sourceEngine, sourceId, sourceType);

        if (existing.isPresent()) {
            throw new DuplicateTransactionException("Transaction already exists for this source");
        }

        XpAccount account = xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseGet(() -> createDefaultAccount(userId));

        int amount = request.amount();
        int balanceBefore = account.getCurrentXp();
        int balanceAfter = balanceBefore + amount;

        if (balanceAfter < 0) {
            throw new InvalidTransactionException("Insufficient XP balance");
        }

        XpTransaction transaction = new XpTransaction();
        transaction.setUserId(userId);
        transaction.setTransactionType(request.transactionType());
        transaction.setAmount(amount);
        transaction.setBalanceAfter(balanceAfter);
        transaction.setSourceEngine(sourceEngine);
        transaction.setSourceId(sourceId);
        transaction.setSourceType(sourceType);
        transaction.setPolicyId(policyId);
        transaction.setMultiplierApplied(multiplierApplied);
        transaction.setBaseAmount(baseAmount);
        transaction.setReason(request.reason());
        transaction.setMetadata(request.metadata() != null ? toJsonString(request.metadata()) : null);

        XpTransaction savedTransaction = xpTransactionRepository.save(transaction);

        account.setCurrentXp(balanceAfter);
        account.setTotalXpEarned(account.getTotalXpEarned() + Math.max(0, amount));
        account.setTotalXpSpent(account.getTotalXpSpent() + Math.max(0, -amount));
        account.setLifetimeXp(account.getTotalXpEarned() - account.getTotalXpSpent());

        int newLevel = calculateLevel(account.getLifetimeXp());
        if (newLevel != account.getCurrentLevel()) {
            int oldLevel = account.getCurrentLevel();
            account.setCurrentLevel(newLevel);
            eventPublisher.publishEvent(new LevelUpEvent(userId, oldLevel, newLevel, (int) calculateXpForLevel(newLevel), Instant.now()));
        }

        account.setLevelProgress(calculateProgress(userId).progressPercentage());

        xpAccountRepository.save(account);

        if (amount > 0) {
            eventPublisher.publishEvent(new XpAwardedEvent(
                    Math.abs(amount), userId, sourceType, sourceId, request.transactionType().name(), Instant.now()));
        } else if (amount < 0) {
            eventPublisher.publishEvent(new XpRemovedEvent(Math.abs(amount), userId, request.reason(), Instant.now()));
        }

        return xpMapper.toTransactionResponse(savedTransaction);
    }

    private XpAccount createDefaultAccount(UUID userId) {
        XpAccount account = new XpAccount();
        account.setUserId(userId);
        account.setCurrentXp(0);
        account.setCurrentLevel(1);
        account.setTotalXpEarned(0);
        account.setTotalXpSpent(0);
        account.setLifetimeXp(0);
        account.setLevelProgress(0.0);
        return xpAccountRepository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(UUID transactionId, UUID userId) {
        XpTransaction transaction = xpTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));
        if (!transaction.getUserId().equals(userId)) {
            throw new XpException("Access denied", "FORBIDDEN", 403);
        }
        return xpMapper.toTransactionResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> listTransactions(UUID userId, Pageable pageable) {
        Page<XpTransaction> transactions = xpTransactionRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId, pageable);
        return transactions.map(xpMapper::toTransactionResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(UUID userId, TransactionHistoryFilter filters) {
        List<XpTransaction> transactions = xpTransactionRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);

        if (filters.transactionType() != null) {
            transactions = transactions.stream()
                    .filter(t -> t.getTransactionType() == filters.transactionType())
                    .toList();
        }

        if (filters.sourceType() != null) {
            transactions = transactions.stream()
                    .filter(t -> filters.sourceType().equals(t.getSourceType()))
                    .toList();
        }

        if (filters.fromDate() != null) {
            Instant from = filters.fromDate();
            transactions = transactions.stream()
                    .filter(t -> t.getCreatedAt().isAfter(from) || t.getCreatedAt().equals(from))
                    .toList();
        }

        if (filters.toDate() != null) {
            Instant to = filters.toDate();
            transactions = transactions.stream()
                    .filter(t -> t.getCreatedAt().isBefore(to) || t.getCreatedAt().equals(to))
                    .toList();
        }

        return transactions.stream()
                .map(xpMapper::toTransactionResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public int calculateLevel(int xp) {
        if (xp < 0) {
            throw new LevelCalculationException("XP cannot be negative");
        }
        int level = 1;
        long xpRequired = 0;
        while (xpRequired <= xp) {
            level++;
            xpRequired = calculateXpForLevel(level);
        }
        return level - 1;
    }

    @Override
    @Transactional(readOnly = true)
    public ProgressResponse calculateProgress(UUID userId) {
        XpAccount account = xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new XpAccountNotFoundException("XP account not found"));

        int currentLevel = account.getCurrentLevel();
        long currentXp = account.getLifetimeXp();
        long xpForCurrentLevel = calculateXpForLevel(currentLevel);
        long xpForNextLevel = calculateXpForLevel(currentLevel + 1);
        long xpInLevel = currentXp - xpForCurrentLevel;
        long xpNeeded = xpForNextLevel - xpForCurrentLevel;
        double progress = xpNeeded > 0 ? Math.min(100.0, (double) xpInLevel / xpNeeded * 100) : 100.0;
        int xpProgress = (int) Math.round(xpInLevel);
        int xpRemaining = (int) Math.round(xpNeeded - xpInLevel);

        return new ProgressResponse(
                currentLevel,
                (int) currentXp,
                (int) xpForCurrentLevel,
                xpProgress,
                xpRemaining,
                Math.round(progress)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public LevelInfo getLevelInfo(int level) {
        if (level < 1) {
            throw new LevelCalculationException("Level must be >= 1");
        }
        long xpRequired = calculateXpForLevel(level);
        long xpForNextLevel = calculateXpForLevel(level + 1);

        return new LevelInfo(
                level,
                (int) xpRequired,
                (int) xpForNextLevel
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AchievementResponse> getAllAchievements() {
        List<AchievementDefinition> definitions = achievementDefinitionRepository.findByDeletedAtIsNullOrderBySortOrderAsc();
        boolean isAdmin = SecurityUtils.isAdmin();
        return definitions.stream()
                .filter(def -> isAdmin || !def.getIsHidden())
                .map(def -> new AchievementResponse(
                        def.getId(),
                        def.getCode(),
                        def.getName(),
                        def.getDescription(),
                        def.getCategory(),
                        def.getIconUrl(),
                        def.getRequirementType(),
                        fromJsonString(def.getRequirementValue(), new TypeReference<Map<String, Object>>() {}),
                        def.getXpReward(),
                        def.getIsHidden(),
                        def.getIsRepeatable(),
                        def.getSortOrder(),
                        def.getCreatedAt()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AchievementResponse getAchievement(UUID achievementId) {
        AchievementDefinition definition = achievementDefinitionRepository.findById(achievementId)
                .orElseThrow(() -> new AchievementNotFoundException("Achievement not found"));
        if (definition.getIsHidden() && !SecurityUtils.isAdmin()) {
            throw new AchievementNotFoundException("Achievement not found");
        }
        return new AchievementResponse(
                definition.getId(),
                definition.getCode(),
                definition.getName(),
                definition.getDescription(),
                definition.getCategory(),
                definition.getIconUrl(),
                definition.getRequirementType(),
                fromJsonString(definition.getRequirementValue(), new TypeReference<Map<String, Object>>() {}),
                definition.getXpReward(),
                definition.getIsHidden(),
                definition.getIsRepeatable(),
                definition.getSortOrder(),
                definition.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAchievementResponse> getUserAchievements(UUID userId) {
        List<UserAchievement> userAchievements = userAchievementRepository.findByUserIdAndDeletedAtIsNull(userId);
        return userAchievements.stream()
                .map(ua -> {
                    AchievementDefinition definition = achievementDefinitionRepository.findById(ua.getAchievementId()).orElse(null);
                    return new UserAchievementResponse(
                            ua.getId(),
                            ua.getUserId(),
                            ua.getAchievementId(),
                            definition != null ? definition.getCode() : "",
                            definition != null ? definition.getName() : "",
                            definition != null ? definition.getCategory() : AchievementCategory.TASK,
                            ua.getCurrentProgress(),
                            ua.getTargetProgress(),
                            ua.getIsUnlocked(),
                            ua.getUnlockedAt(),
                            fromJsonString(ua.getProgressMetadata(), new TypeReference<Map<String, Object>>() {}),
                            ua.getCreatedAt()
                    );
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserAchievementResponse getUserAchievement(UUID userId, UUID userAchievementId) {
        UserAchievement userAchievement = userAchievementRepository
                .findByUserIdAndAchievementIdAndDeletedAtIsNull(userId, userAchievementId)
                .orElseThrow(() -> new AchievementNotFoundException("User achievement not found"));

        AchievementDefinition definition = achievementDefinitionRepository.findById(userAchievement.getAchievementId())
                .orElse(null);

        return new UserAchievementResponse(
                userAchievement.getId(),
                userAchievement.getUserId(),
                userAchievement.getAchievementId(),
                definition != null ? definition.getCode() : "",
                definition != null ? definition.getName() : "",
                definition != null ? definition.getCategory() : AchievementCategory.TASK,
                userAchievement.getCurrentProgress(),
                userAchievement.getTargetProgress(),
                userAchievement.getIsUnlocked(),
                userAchievement.getUnlockedAt(),
                fromJsonString(userAchievement.getProgressMetadata(), new TypeReference<Map<String, Object>>() {}),
                userAchievement.getCreatedAt()
        );
    }

    @Override
    @Transactional
    public List<AchievementResponse> checkAchievements(UUID userId) {
        List<AchievementResponse> unlocked = new ArrayList<>();
        List<AchievementDefinition> definitions = achievementDefinitionRepository.findByDeletedAtIsNullOrderBySortOrderAsc();

        for (AchievementDefinition definition : definitions) {
            UserAchievement userAchievement = userAchievementRepository
                    .findByUserIdAndAchievementIdAndDeletedAtIsNull(userId, definition.getId())
                    .orElse(null);

            if (userAchievement == null) {
                userAchievement = new UserAchievement();
                userAchievement.setUserId(userId);
                userAchievement.setAchievementId(definition.getId());
                userAchievement.setCurrentProgress(0);
                userAchievement.setTargetProgress(100);
                userAchievement.setIsUnlocked(false);
                userAchievementRepository.save(userAchievement);
            }

            if (!userAchievement.getIsUnlocked() || definition.getIsRepeatable()) {
                int newProgress = evaluateAchievementProgress(definition, userId);
                if (newProgress > userAchievement.getCurrentProgress()) {
                    userAchievement.setCurrentProgress(newProgress);
                    eventPublisher.publishEvent(new AchievementProgressUpdatedEvent(
                            userId, definition.getId(), newProgress, userAchievement.getTargetProgress(), Instant.now()));
                }

                if (newProgress >= userAchievement.getTargetProgress() && !userAchievement.getIsUnlocked()) {
                    userAchievement.setIsUnlocked(true);
                    userAchievement.setUnlockedAt(Instant.now());
                    userAchievementRepository.save(userAchievement);

                    eventPublisher.publishEvent(new AchievementUnlockedEvent(
                            userId, definition.getId(), definition.getCode(), definition.getXpReward(), Instant.now()));

                    unlocked.add(getAchievement(definition.getId()));
                }
            }
        }

        return unlocked;
    }

    @Override
    @Transactional
    public UserAchievementResponse unlockAchievement(UUID userId, UUID achievementId) {
        AchievementDefinition definition = achievementDefinitionRepository.findById(achievementId)
                .orElseThrow(() -> new AchievementNotFoundException("Achievement not found"));

        UserAchievement userAchievement = userAchievementRepository
                .findByUserIdAndAchievementIdAndDeletedAtIsNull(userId, achievementId)
                .orElseGet(() -> {
                    UserAchievement ua = new UserAchievement();
                    ua.setUserId(userId);
                    ua.setAchievementId(achievementId);
                    ua.setCurrentProgress(0);
                    ua.setTargetProgress(100);
                    ua.setIsUnlocked(false);
                    return userAchievementRepository.save(ua);
                });

        if (!userAchievement.getIsUnlocked()) {
            userAchievement.setIsUnlocked(true);
            userAchievement.setUnlockedAt(Instant.now());
            userAchievement.setCurrentProgress(userAchievement.getTargetProgress());
            userAchievementRepository.save(userAchievement);

            eventPublisher.publishEvent(new AchievementUnlockedEvent(
                    userId, achievementId, definition.getCode(), definition.getXpReward(), Instant.now()));
        }

        return new UserAchievementResponse(
                userAchievement.getId(),
                userAchievement.getUserId(),
                userAchievement.getAchievementId(),
                definition.getCode(),
                definition.getName(),
                definition.getCategory(),
                userAchievement.getCurrentProgress(),
                userAchievement.getTargetProgress(),
                userAchievement.getIsUnlocked(),
                userAchievement.getUnlockedAt(),
                fromJsonString(userAchievement.getProgressMetadata(), new TypeReference<Map<String, Object>>() {}),
                userAchievement.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyResponse> getAllPolicies() {
        List<XpPolicy> policies = xpPolicyRepository.findByDeletedAtIsNullOrderByPriorityDesc();
        return policies.stream()
                .map(p -> new PolicyResponse(
                        p.getId(),
                        p.getCode(),
                        p.getName(),
                        p.getDescription(),
                        p.getPolicyType(),
                        p.getBaseXp(),
                        p.getMultiplier(),
                        fromJsonString(p.getConditions(), new TypeReference<Map<String, Object>>() {}),
                        p.getIsActive(),
                        p.getPriority(),
                        p.getCreatedAt(),
                        p.getUpdatedAt()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyResponse getPolicy(UUID policyId) {
        XpPolicy policy = xpPolicyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException("Policy not found"));
        return new PolicyResponse(
                policy.getId(),
                policy.getCode(),
                policy.getName(),
                policy.getDescription(),
                policy.getPolicyType(),
                policy.getBaseXp(),
                policy.getMultiplier(),
                fromJsonString(policy.getConditions(), new TypeReference<Map<String, Object>>() {}),
                policy.getIsActive(),
                policy.getPriority(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }

    @Override
    @Transactional
    public PolicyResponse createPolicy(PolicyRequest request) {
        if (!SecurityUtils.isAdmin()) {
            throw new XpException("Admin access required", "FORBIDDEN", 403);
        }

        XpPolicy policy = new XpPolicy();
        policy.setCode(request.code());
        policy.setName(request.name());
        policy.setDescription(request.description());
        policy.setPolicyType(request.policyType());
        policy.setBaseXp(request.baseXp());
        policy.setMultiplier(request.multiplier());
        policy.setConditions(toJsonString(request.conditions()));
        policy.setIsActive(true);
        policy.setPriority(request.priority() != null ? request.priority() : 0);

        XpPolicy saved = xpPolicyRepository.save(policy);
        eventPublisher.publishEvent(new PolicyChangedEvent(
                saved.getId(), saved.getCode(), "created", Instant.now()));

        return getPolicy(saved.getId());
    }

    @Override
    @Transactional
    public PolicyResponse updatePolicy(UUID policyId, PolicyRequest request) {
        if (!SecurityUtils.isAdmin()) {
            throw new XpException("Admin access required", "FORBIDDEN", 403);
        }

        XpPolicy policy = xpPolicyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException("Policy not found"));

        policy.setName(request.name());
        policy.setDescription(request.description());
        policy.setBaseXp(request.baseXp());
        policy.setMultiplier(request.multiplier());
        policy.setConditions(toJsonString(request.conditions()));
        policy.setIsActive(request.isActive());
        policy.setPriority(request.priority() != null ? request.priority() : 0);

        XpPolicy saved = xpPolicyRepository.save(policy);
        eventPublisher.publishEvent(new PolicyChangedEvent(
                saved.getId(), saved.getCode(), "updated", Instant.now()));

        return getPolicy(saved.getId());
    }

    @Override
    @Transactional
    public void deletePolicy(UUID policyId) {
        if (!SecurityUtils.isAdmin()) {
            throw new XpException("Admin access required", "FORBIDDEN", 403);
        }

        XpPolicy policy = xpPolicyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException("Policy not found"));

        policy.setDeletedAt(Instant.now());
        xpPolicyRepository.save(policy);

        eventPublisher.publishEvent(new PolicyChangedEvent(
                policyId, policy.getCode(), "deleted", Instant.now()));
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyEvaluationResponse evaluatePolicies(UUID userId) {
        List<XpPolicy> activePolicies = xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true);

        double totalMultiplier = 1.0;
        int baseXp = 0;
        List<String> appliedPolicies = new ArrayList<>();
        boolean primaryBaseXpSet = false;

        for (XpPolicy policy : activePolicies) {
            if (matchesPolicy(policy, userId)) {
                if (!primaryBaseXpSet) {
                    baseXp += policy.getBaseXp();
                    primaryBaseXpSet = true;
                }
                totalMultiplier *= policy.getMultiplier();
                appliedPolicies.add(policy.getCode());
            }
        }

        totalMultiplier = Math.min(totalMultiplier, 10.0);
        int calculatedXp = (int) Math.round(baseXp * totalMultiplier);

        return new PolicyEvaluationResponse(
                "combined",
                "Combined Policy Evaluation",
                true,
                totalMultiplier,
                baseXp,
                calculatedXp,
                Map.of("appliedPolicies", appliedPolicies)
        );
    }

    @Override
    public XpCalculationResult calculateXpForEvent(UUID userId, Map<String, Object> context, XpService.XpSourceType sourceType) {
        List<XpPolicy> activePolicies = xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true);

        XpPolicy primaryPolicy = null;
        double totalMultiplier = 1.0;
        PolicyType basePolicyType = sourceType.getBasePolicyType();

        for (XpPolicy policy : activePolicies) {
            if (matchesPolicy(policy, userId)) {
                if (primaryPolicy == null && (basePolicyType == null || policy.getPolicyType() == basePolicyType)) {
                    primaryPolicy = policy;
                }
                totalMultiplier *= calculateMultiplierForPolicy(policy, context);
            }
        }

        int baseXp;
        if (sourceType == XpService.XpSourceType.GOAL && context.containsKey("goalEstimatedXp")) {
            Object estimatedXpObj = context.get("goalEstimatedXp");
            if (estimatedXpObj instanceof Number estimatedXpNumber) {
                baseXp = estimatedXpNumber.intValue();
            } else {
                baseXp = 0;
            }
            if (baseXp <= 0 && primaryPolicy != null && primaryPolicy.getPolicyType() == PolicyType.GOAL_COMPLETION) {
                baseXp = primaryPolicy.getBaseXp();
            }
        } else if (primaryPolicy != null) {
            baseXp = primaryPolicy.getBaseXp();
        } else {
            baseXp = 0;
        }

        totalMultiplier = Math.min(totalMultiplier, 10.0);
        int finalXp = (int) Math.round(baseXp * totalMultiplier);

        return new XpCalculationResult(
                primaryPolicy != null ? primaryPolicy.getId() : null,
                baseXp,
                totalMultiplier,
                finalXp
        );
    }

    @Override
    @Transactional
    public RewardResponse calculateReward(RewardCalculationRequest request) {
        UUID userId = SecurityUtils.getCurrentUserId();

        XpCalculationResult calculation = calculateXpForEvent(userId, Map.of(), XpSourceType.REWARD);

        return new RewardResponse(
                null,
                userId,
                request.rewardType(),
                request.sourceType(),
                request.sourceId(),
                calculation.finalXp(),
                null,
                calculation.multiplier(),
                calculation.baseXp(),
                Instant.now(),
                Map.of()
        );
    }

    @Override
    @Transactional
    public RewardResponse grantReward(UUID userId, UUID rewardId) {
        XpCalculationResult calculation = calculateXpForEvent(userId, Map.of(), XpSourceType.REWARD);

        TransactionCreateRequest request = new TransactionCreateRequest(
                TransactionType.BONUS,
                calculation.finalXp(),
                "xp-engine",
                rewardId,
                "REWARD",
                "Reward granted",
                Map.of()
        );

        TransactionResponse transactionResponse = createTransactionInternal(userId, request,
                calculation.primaryPolicyId(), calculation.multiplier(), calculation.baseXp());

        RewardHistory rewardHistory = new RewardHistory();
        rewardHistory.setUserId(userId);
        rewardHistory.setRewardType(com.thesystem.modules.xp.enums.RewardType.ADMIN);
        rewardHistory.setSourceType("REWARD");
        rewardHistory.setSourceId(rewardId);
        rewardHistory.setXpAmount(calculation.finalXp());
        rewardHistory.setPolicyId(calculation.primaryPolicyId());
        rewardHistory.setMultiplierApplied(calculation.multiplier());
        rewardHistory.setBaseAmount(calculation.baseXp());
        rewardHistoryRepository.save(rewardHistory);

        return new RewardResponse(
                rewardHistory.getId(),
                userId,
                "MANUAL",
                "REWARD",
                rewardId,
                calculation.finalXp(),
                calculation.primaryPolicyId(),
                calculation.multiplier(),
                calculation.baseXp(),
                Instant.now(),
                Map.of()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<RewardHistoryResponse> getRewardHistory(UUID userId) {
        List<RewardHistory> rewards = rewardHistoryRepository.findByUserIdAndDeletedAtIsNullOrderByAwardedAtDesc(userId);
        return rewards.stream()
                .map(r -> new RewardHistoryResponse(
                        r.getId(),
                        r.getUserId(),
                        r.getRewardType().name(),
                        r.getSourceType(),
                        r.getSourceId(),
                        r.getXpAmount(),
                        r.getPolicyId(),
                        r.getMultiplierApplied(),
                        r.getBaseAmount(),
                        r.getAwardedAt(),
                        fromJsonString(r.getMetadata(), new TypeReference<Map<String, Object>>() {}),
                        r.getCreatedAt()
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StatisticsResponse getStatistics() {
        UUID userId = SecurityUtils.getCurrentUserId();

        XpAccount account = xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElse(new XpAccount());

        Instant now = Instant.now();
        java.time.ZoneId zoneId = userTimezoneResolver.resolveUserZoneId(userId);
        java.time.LocalDate today = java.time.LocalDate.now(zoneId);
        java.time.LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        java.time.LocalDate monthStart = today.withDayOfMonth(1);

        Instant dayStart = today.atStartOfDay(zoneId).toInstant();
        Instant weekStartInstant = weekStart.atStartOfDay(zoneId).toInstant();
        Instant monthStartInstant = monthStart.atStartOfDay(zoneId).toInstant();

        Integer dailyXp = xpTransactionRepository.sumPositiveAmountByUserIdAndCreatedAtAfter(userId, dayStart);
        Integer weeklyXp = xpTransactionRepository.sumPositiveAmountByUserIdAndCreatedAtAfter(userId, weekStartInstant);
        Integer monthlyXp = xpTransactionRepository.sumPositiveAmountByUserIdAndCreatedAtAfter(userId, monthStartInstant);

        long tasksCompleted = xpTransactionRepository.findByUserIdAndTransactionTypeAndDeletedAtIsNull(userId, TransactionType.TASK_COMPLETION)
                .stream().map(XpTransaction::getSourceId).distinct().count();
        long goalsCompleted = xpTransactionRepository.findByUserIdAndTransactionTypeAndDeletedAtIsNull(userId, TransactionType.GOAL_COMPLETION)
                .stream().map(XpTransaction::getSourceId).distinct().count();
        long achievementsUnlocked = userAchievementRepository.countByUserIdAndIsUnlockedAndDeletedAtIsNull(userId, true);

        return new StatisticsResponse(
                dailyXp != null ? dailyXp : 0,
                weeklyXp != null ? weeklyXp : 0,
                monthlyXp != null ? monthlyXp : 0,
                account.getLifetimeXp(),
                account.getCurrentLevel(),
                account.getLevelProgress(),
                (int) tasksCompleted,
                (int) goalsCompleted,
                (int) achievementsUnlocked,
                Map.of(),
                Map.of(),
                Map.of(),
                now
        );
    }

    @Override
    @Transactional(readOnly = true)
    public LeaderboardResponse getLeaderboard(Pageable pageable) {
        List<XpAccount> allAccounts = xpAccountRepository.findByDeletedAtIsNullOrderByLifetimeXpDesc();
        long totalElements = allAccounts.size();

        List<LeaderboardEntry> entries = allAccounts.stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .map(a -> new LeaderboardEntry(
                        a.getUserId(),
                        "User " + a.getUserId().toString().substring(0, 8),
                        a.getLifetimeXp(),
                        a.getCurrentLevel(),
                        0
                ))
                .collect(Collectors.toList());

        int rank = (int) pageable.getOffset() + 1;
        for (int i = 0; i < entries.size(); i++) {
            LeaderboardEntry entry = entries.get(i);
            entries.set(i, new LeaderboardEntry(
                    entry.userId(),
                    entry.username(),
                    entry.currentXp(),
                    entry.currentLevel(),
                    rank + i
            ));
        }

        int totalPages = (int) Math.ceil((double) totalElements / pageable.getPageSize());
        if (totalPages == 0) totalPages = 1;

        return new LeaderboardResponse(entries, totalPages, totalElements);
    }

    private long calculateXpForLevel(int level) {
        return Math.round(100 * Math.pow(level, 1.5));
    }

    private int evaluateAchievementProgress(AchievementDefinition definition, UUID userId) {
        UserAchievement userAchievement = userAchievementRepository
                .findByUserIdAndAchievementIdAndDeletedAtIsNull(userId, definition.getId())
                .orElse(null);
        return userAchievement != null ? userAchievement.getCurrentProgress() : 0;
    }

    public double calculatePolicyMultiplier(UUID userId, Map<String, Object> context) {
        if (context == null) {
            context = Map.of();
        }

        List<XpPolicy> activePolicies = xpPolicyRepository.findByIsActiveAndDeletedAtIsNullOrderByPriorityDesc(true);
        double totalMultiplier = 1.0;

        for (XpPolicy policy : activePolicies) {
            if (matchesPolicy(policy, userId)) {
                totalMultiplier *= calculateMultiplierForPolicy(policy, context);
            }
        }

        return Math.min(totalMultiplier, 10.0);
    }

    private double calculateMultiplierForPolicy(XpPolicy policy, Map<String, Object> context) {
        String conditions = policy.getConditions();
        if (conditions == null || conditions.isBlank()) {
            return policy.getMultiplier();
        }

        Map<String, Object> conditionsMap;
        try {
            conditionsMap = objectMapper.readValue(conditions, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return policy.getMultiplier();
        }

        double multiplier = policy.getMultiplier();

        if (conditionsMap.containsKey("priority_multipliers") && context.containsKey("taskPriority")) {
            Object taskPriority = context.get("taskPriority");
            if (taskPriority instanceof String) {
                Object priorityObj = conditionsMap.get("priority_multipliers");
                if (priorityObj instanceof Map) {
                    Map<?, ?> priorityMap = (Map<?, ?>) priorityObj;
                    Object multObj = priorityMap.get(taskPriority);
                    if (multObj instanceof Number) {
                        multiplier *= ((Number) multObj).doubleValue();
                    }
                }
            }
        }

        if (conditionsMap.containsKey("difficulty_multipliers")) {
            String difficultyKey = null;
            if (context.containsKey("taskDifficulty") && context.get("taskDifficulty") instanceof String) {
                difficultyKey = (String) context.get("taskDifficulty");
            } else if (context.containsKey("goalDifficulty") && context.get("goalDifficulty") instanceof String) {
                difficultyKey = (String) context.get("goalDifficulty");
            }

            if (difficultyKey != null) {
                Object difficultyObj = conditionsMap.get("difficulty_multipliers");
                if (difficultyObj instanceof Map) {
                    Map<?, ?> difficultyMap = (Map<?, ?>) difficultyObj;
                    Object multObj = difficultyMap.get(difficultyKey);
                    if (multObj instanceof Number) {
                        multiplier *= ((Number) multObj).doubleValue();
                    }
                }
            }
        }

        if (conditionsMap.containsKey("streak_bonus") && context.containsKey("streak")) {
            Object streakObj = context.get("streak");
            if (streakObj instanceof Integer) {
                int streak = (Integer) streakObj;
                Object streakConfigObj = conditionsMap.get("streak_bonus");
                if (streakConfigObj instanceof Map) {
                    Map<?, ?> streakConfig = (Map<?, ?>) streakConfigObj;
                    Object enabledObj = streakConfig.get("enabled");
                    if (Boolean.TRUE.equals(enabledObj)) {
                        Object milestonesObj = streakConfig.get("milestones");
                        Object multipliersObj = streakConfig.get("multipliers");

                        if (milestonesObj instanceof List && multipliersObj instanceof List) {
                            List<?> milestonesList = (List<?>) milestonesObj;
                            List<?> multipliersList = (List<?>) multipliersObj;

                            int[] milestones = new int[milestonesList.size()];
                            for (int i = 0; i < milestonesList.size(); i++) {
                                if (milestonesList.get(i) instanceof Number) {
                                    milestones[i] = ((Number) milestonesList.get(i)).intValue();
                                }
                            }

                            double[] multipliers = new double[multipliersList.size()];
                            for (int i = 0; i < multipliersList.size(); i++) {
                                if (multipliersList.get(i) instanceof Number) {
                                    multipliers[i] = ((Number) multipliersList.get(i)).doubleValue();
                                }
                            }

                            for (int i = milestones.length - 1; i >= 0; i--) {
                                if (streak >= milestones[i]) {
                                    multiplier *= multipliers[i];
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return multiplier;
    }

    private boolean matchesPolicy(XpPolicy policy, UUID userId) {
        if (policy.getIsActive() == null || !policy.getIsActive()) {
            return false;
        }

        String conditions = policy.getConditions();
        if (conditions == null || conditions.isBlank()) {
            return true;
        }

        Map<String, Object> conditionsMap;
        try {
            conditionsMap = objectMapper.readValue(conditions, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return false;
        }

        if (conditionsMap.containsKey("min_user_level")) {
            Object value = conditionsMap.get("min_user_level");
            if (!(value instanceof Number)) {
                return false;
            }
            int minLevel = ((Number) value).intValue();
            XpAccount account = xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId).orElse(null);
            if (account == null || account.getCurrentLevel() < minLevel) {
                return false;
            }
        }

        if (conditionsMap.containsKey("max_user_level")) {
            Object value = conditionsMap.get("max_user_level");
            if (!(value instanceof Number)) {
                return false;
            }
            int maxLevel = ((Number) value).intValue();
            XpAccount account = xpAccountRepository.findByUserIdAndDeletedAtIsNull(userId).orElse(null);
            if (account == null || account.getCurrentLevel() > maxLevel) {
                return false;
            }
        }

        if (conditionsMap.containsKey("allowed_user_ids")) {
            Object value = conditionsMap.get("allowed_user_ids");
            if (!(value instanceof List<?>)) {
                return false;
            }
            List<?> allowedIds = (List<?>) value;
            boolean allowed = allowedIds.stream()
                    .anyMatch(id -> userId.toString().equals(id.toString()));
            if (!allowed) {
                return false;
            }
        }

        if (conditionsMap.containsKey("excluded_user_ids")) {
            Object value = conditionsMap.get("excluded_user_ids");
            if (!(value instanceof List<?>)) {
                return false;
            }
            List<?> excludedIds = (List<?>) value;
            boolean excluded = excludedIds.stream()
                    .anyMatch(id -> userId.toString().equals(id.toString()));
            if (excluded) {
                return false;
            }
        }

        return true;
    }

    private String toJsonString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private <T> T fromJsonString(String json, TypeReference<T> type) {
        if (json == null || json.isBlank()) {
            return type.getType().equals(new TypeReference<Map<String, Object>>() {}.getType())
                    ? (T) Map.of()
                    : null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            return type.getType().equals(new TypeReference<Map<String, Object>>() {}.getType())
                    ? (T) Map.of()
                    : null;
        }
    }

    private String getLevelTitle(int level) {
        if (level <= 1) return "Novice";
        if (level <= 3) return "Beginner";
        if (level <= 5) return "Intermediate";
        if (level <= 7) return "Advanced";
        if (level <= 10) return "Expert";
        return "Master";
    }
}
