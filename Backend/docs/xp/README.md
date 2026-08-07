# XP Engine

The measurement layer of THE SYSTEM that quantifies verified effort across all engines and converts user actions into measurable progress.

## Purpose

The XP Engine serves as the quantification layer for user effort. While the Goal Engine defines what users want to achieve and the Task Engine defines how users reach those goals, the XP Engine measures the value and effort invested.

Core capabilities:
- Immutable XP transaction ledger
- Current XP calculation derived from transactions
- Level progression system with carry-over mechanics
- Achievement definitions and progress tracking
- Configurable reward policies and multipliers
- Reward history with full audit trail
- XP analytics and reporting

## Responsibilities

### Owned by XP Engine

- **XP Accounts**: User XP balances (cached, derived from transactions)
- **XP Ledger**: Immutable transaction history for all XP changes
- **XP Transactions**: Every XP modification recorded as an immutable transaction
- **Level Calculation**: Current level, XP required, carry-over XP
- **Achievement Definitions**: Catalog of available achievements
- **User Achievements**: Progress tracking and unlock state per user
- **XP Policies**: Configurable reward rules and base XP values
- **Reward Multipliers**: Dynamic multipliers for difficulty, streaks, bonuses
- **Reward History**: Complete history of rewards issued
- **XP Statistics**: Daily, weekly, monthly, lifetime XP tracking

### Not Owned by XP Engine

- Tasks (owned by Task Engine)
- Goals (owned by Goal Engine)
- Task completion logic (owned by Task Engine)
- Goal validation (owned by Goal Engine)
- Health data (owned by Health Engine)
- Memory storage (owned by Memory Engine)
- Notifications (owned by Notification Engine)
- AI suggestions (owned by AI Engine)
- Analytics computation (owned by Analytics Engine)

## Database

### Tables

| Table | Purpose |
|-------|---------|
| `xp_accounts` | User XP balances and level (cached) |
| `xp_transactions` | Immutable transaction history for all XP changes |
| `achievement_definitions` | Catalog of available achievements |
| `user_achievements` | Per-user achievement progress and unlock state |
| `xp_policies` | Configurable reward rules and base XP values |
| `reward_history` | Query-optimized reward issuance history |

### Key Relationships

```
users (1) ─── (1) xp_accounts
users (1) ─── (N) xp_transactions
users (1) ─── (N) user_achievements
achievement_definitions (1) ─── (N) user_achievements
xp_policies (1) ─── (N) xp_transactions
users (1) ─── (N) reward_history
```

### Indexes

```sql
-- XP Accounts
CREATE INDEX idx_xp_accounts_user_id ON xp_accounts(user_id);
CREATE INDEX idx_xp_accounts_current_level ON xp_accounts(current_level) WHERE deleted_at IS NULL;

-- XP Transactions
CREATE INDEX idx_xp_transactions_user_id ON xp_transactions(user_id);
CREATE INDEX idx_xp_transactions_created_at ON xp_transactions(created_at);
CREATE INDEX idx_xp_transactions_type ON xp_transactions(transaction_type);
CREATE INDEX idx_xp_transactions_source ON xp_transactions(source_engine, source_id) WHERE source_id IS NOT NULL;

-- Achievement Definitions
CREATE INDEX idx_achievement_definitions_code ON achievement_definitions(code) WHERE deleted_at IS NULL;
CREATE INDEX idx_achievement_definitions_category ON achievement_definitions(category) WHERE deleted_at IS NULL;

-- User Achievements
CREATE INDEX idx_user_achievements_user_id ON user_achievements(user_id);
CREATE INDEX idx_user_achievements_achievement_id ON user_achievements(achievement_id);
CREATE INDEX idx_user_achievements_unlocked ON user_achievements(is_unlocked, unlocked_at) WHERE is_unlocked = TRUE;

-- XP Policies
CREATE INDEX idx_xp_policies_code ON xp_policies(code) WHERE deleted_at IS NULL AND is_active = TRUE;
CREATE INDEX idx_xp_policies_type ON xp_policies(policy_type) WHERE deleted_at IS NULL AND is_active = TRUE;

-- Reward History
CREATE INDEX idx_reward_history_user_id ON reward_history(user_id);
CREATE INDEX idx_reward_history_source ON reward_history(source_type, source_id);
CREATE INDEX idx_reward_history_awarded_at ON reward_history(awarded_at);
```

## XP Ledger

The XP Ledger is an immutable transaction history for all XP changes. Current XP is derived from transactions; never overwrite history.

### Core Principles

- XP is never modified directly
- Every XP change creates an immutable XP Transaction
- Current XP is derived from transactions
- Transaction records are never updated or deleted

### Balance Derivation

```
current_xp = SUM(amount) WHERE user_id = :userId AND deleted_at IS NULL
current_level = calculate_level(current_xp)
level_progress = calculate_progress(current_xp, current_level)
```

The `xp_accounts` table is a cache for performance. It is updated atomically with transaction creation.

## Transactions

### Transaction Types

| Type | Description | Amount |
|------|-------------|--------|
| `TASK_COMPLETION` | XP awarded for completing a task | Positive |
| `GOAL_COMPLETION` | XP awarded for completing a goal | Positive |
| `ACHIEVEMENT` | XP awarded for unlocking an achievement | Positive |
| `ADMIN_ADJUSTMENT` | Manual XP adjustment by admin | Positive or Negative |
| `PENALTY` | XP removed as penalty | Negative |
| `BONUS` | Bonus XP from special events | Positive |
| `STREAK_BONUS` | XP bonus for maintaining streaks | Positive |
| `LEVEL_UP` | Record of level-up event | Zero (audit only) |
| `SYSTEM` | System-initiated transaction | Varies |

### Creation Flow

1. External event received (e.g., `TaskCompletedEvent`)
2. XP Engine validates event authenticity
3. XP Engine evaluates applicable XP policies
4. XP Engine calculates final XP amount (base + multipliers)
5. XP Engine creates XP Transaction record
6. XP Engine updates `xp_accounts` (cached values)
7. XP Engine checks for level up
8. XP Engine checks for achievement progress
9. XP Engine publishes domain events
10. XP Engine returns result to caller

### Transaction Metadata

Each transaction contains:
- `transaction_type`: Category of XP change
- `amount`: XP delta (positive or negative)
- `balance_after`: XP balance after this transaction
- `source_engine`: Which engine triggered the reward
- `source_id`: ID of the source entity (task_id, goal_id, etc.)
- `source_type`: Type of source entity
- `policy_id`: Which policy was applied
- `multiplier_applied`: Final multiplier used
- `base_amount`: XP before multipliers
- `reason`: Human-readable explanation
- `metadata`: Additional structured data

### Idempotency

To prevent duplicate rewards:
- Each source entity can only trigger one XP transaction per event
- Duplicate detection uses `(source_engine, source_id, source_type)` composite key within a time window
- Idempotency tokens stored in transaction metadata
- Replay of same event results in no-op (returns existing transaction)

## Level System

### Level Design

| Level | XP Required | Cumulative XP | Description |
|-------|-------------|---------------|-------------|
| 1 | 0 | 0 | Starting level |
| 2 | 100 | 100 | Beginner |
| 3 | 250 | 350 | Novice |
| 4 | 500 | 850 | Intermediate |
| 5 | 1,000 | 1,850 | Advanced |
| 6 | 1,750 | 3,550 | Expert |
| 7 | 2,750 | 6,300 | Proficient |
| 8 | 4,000 | 10,300 | Master |
| 9 | 5,500 | 15,800 | Elite |
| 10 | 7,500 | 23,300 | Legendary |
| 11+ | +10,000 per level | Progressive | Ascending |

### Calculation Algorithm

Formula: `xp_required_for_level = 100 * level^1.5` (rounded to nearest 25)

```java
public class LevelCalculator {
    public int calculateLevel(long totalXp) {
        int level = 1;
        long xpRequired = 0;
        while (xpRequired <= totalXp) {
            level++;
            xpRequired = calculateXpForLevel(level);
        }
        return level - 1;
    }

    public long calculateXpForLevel(int level) {
        return Math.round(100 * Math.pow(level, 1.5));
    }

    public double calculateProgress(long currentXp, int currentLevel) {
        long xpForCurrentLevel = calculateXpForLevel(currentLevel);
        long xpForNextLevel = calculateXpForLevel(currentLevel + 1);
        long xpInLevel = currentXp - xpForCurrentLevel;
        long xpNeeded = xpForNextLevel - xpForCurrentLevel;
        return Math.min(100.0, (double) xpInLevel / xpNeeded * 100);
    }
}
```

### Carry-Over

When leveling up, excess XP carries to the next level. Level-up detection is triggered when a transaction causes balance to exceed the threshold.

### Prestige Support (Future)

Prestige resets level but grants permanent multipliers:
- Prestige rank tracked separately from level
- Each prestige adds 10% base XP multiplier
- Prestige achievements unlocked at milestones
- Architecture supports this via `prestige_rank` column in `xp_accounts` (future migration)

## Achievements

### Categories

| Category | Description | Examples |
|----------|-------------|----------|
| `TASK` | Task-based achievements | Complete 100 tasks |
| `GOAL` | Goal-based achievements | Complete 10 goals |
| `STREAK` | Consistency achievements | 30-day streak |
| `SPEED` | Speed achievements | Complete task in under 5 minutes |
| `EXPLORER` | Diversity achievements | Use 5 different task types |
| `MASTER` | Mastery achievements | Reach level 50 |
| `SPECIAL` | Time-limited or event achievements | Holiday challenges |

### Requirement Types

| Type | Description | Example |
|------|-------------|---------|
| `counter` | Count occurrences | Complete 100 tasks |
| `streak` | Consecutive days | 30-day streak |
| `milestone` | Reach specific value | Earn 10,000 XP |
| `collection` | Collect variety | Use 5 task types |
| `speed` | Complete within time | Task in under 5 minutes |
| `custom` | Complex logic | Complete 10 tasks in one day |

### Progress Tracking

Progress is updated incrementally:
- Each relevant event updates `current_progress`
- Progress never decreases (except for repeatable achievements with reset)
- `is_unlocked` set when `current_progress >= target_progress`
- `unlocked_at` recorded on unlock

### Visibility

- `is_hidden`: Achievements not shown until unlocked
- `is_repeatable`: Can be earned multiple times
- `sort_order`: Controls display order

### Rewards

When an achievement is unlocked:
- XP reward issued via XP Transaction
- `AchievementUnlocked` event published
- Notification Engine sends notification
- Analytics Engine records achievement statistics

## Policies

### Policy Types

| Type | Scope | Example |
|------|-------|---------|
| `TASK_COMPLETION` | Per-task reward | +10 XP per completed task |
| `GOAL_COMPLETION` | Per-goal reward | +100 XP per completed goal |
| `DIFFICULTY_MULTIPLIER` | Task/goal difficulty | 2x for EXTREME difficulty |
| `STREAK_MULTIPLIER` | Consecutive completions | 1.5x for 7-day streak |
| `PRIORITY_MULTIPLIER` | Task priority | 1.25x for HIGH priority |
| `BONUS` | Special events | Double XP weekend |
| `PENALTY` | Missed deadlines | -5 XP per missed daily task |

### Evaluation Order

When multiple policies apply:
1. Policies evaluated by `priority` (higher priority first)
2. Base XP from primary policy
3. Multipliers applied cumulatively
4. Final amount = base_xp * multiplier1 * multiplier2 * ...

### Ownership

- **XP Engine**: Owns policy evaluation logic, policy registry, multiplier application
- **Settings Engine**: Stores policy configuration values (enabled/disabled, custom values)
- **Admin**: Can create/modify policies via Settings Engine

## Analytics

### Statistics Available

| Statistic | Description | Calculation |
|-----------|-------------|-------------|
| `daily_xp` | XP earned today | SUM(amount) FROM xp_transactions WHERE date = TODAY() |
| `weekly_xp` | XP earned this week | SUM(amount) FROM xp_transactions WHERE week = THIS_WEEK() |
| `monthly_xp` | XP earned this month | SUM(amount) FROM xp_transactions WHERE month = THIS_MONTH() |
| `lifetime_xp` | Total XP earned | SUM(amount) FROM xp_transactions WHERE amount > 0 |
| `current_level` | Current level | Calculated from lifetime_xp |
| `level_progress` | Progress to next level | Percentage to next level threshold |
| `tasks_completed` | Tasks that generated XP | COUNT(DISTINCT source_id) WHERE source_type = 'TASK' |
| `goals_completed` | Goals that generated XP | COUNT(DISTINCT source_id) WHERE source_type = 'GOAL' |
| `achievements_unlocked` | Total achievements unlocked | COUNT(*) FROM user_achievements WHERE is_unlocked = TRUE |

### Caching Strategy

- User-level statistics cached for 5 minutes
- Global statistics cached for 15 minutes
- Cache invalidated on new XP transaction

### Breakdown Categories

- `by_source`: XP by source engine (Task, Goal, etc.)
- `by_type`: XP by transaction type
- `by_achievement`: XP by achievement category
- `by_time`: XP by hour of day, day of week
- `by_policy`: XP by policy applied

## Reward Flow

### Calculation

```
base_xp = policy.TASK_COMPLETION.base_xp
priority_multiplier = policy.PRIORITY_MULTIPLIER[task.priority]
difficulty_multiplier = policy.DIFFICULTY_MULTIPLIER[task.difficulty]
streak_multiplier = calculate_streak_multiplier(user_id)
final_xp = base_xp * priority_multiplier * difficulty_multiplier * streak_multiplier
```

### Granting

Each reward creates two records:
1. Entry in `xp_transactions` (immutable ledger)
2. Entry in `reward_history` (query-optimized history)

This denormalization enables fast queries for recent rewards, efficient analytics on reward patterns, and complete audit trail in `xp_transactions`.

### Multiplier Capping

- Maximum combined multiplier: 10.0x
- Prevents exploit via multiplier stacking
- Configurable via XP policy

## Events

### Published Events

| Event | Trigger | Payload |
|-------|---------|---------|
| `XPAwarded` | XP transaction created | xp_amount, user_id, source_type, source_id, transaction_type |
| `XPRemoved` | XP penalty or reversal | xp_amount, user_id, reason |
| `LevelUp` | User levels up | user_id, old_level, new_level, xp_required |
| `AchievementUnlocked` | Achievement unlocked | user_id, achievement_id, achievement_code, xp_reward |
| `AchievementProgressUpdated` | Achievement progress changed | user_id, achievement_id, current_progress, target_progress |
| `PolicyChanged` | XP policy modified | policy_id, policy_code, changes |
| `XPAdjusted` | Admin manual adjustment | user_id, amount, reason, admin_id |

### Consumed Events

| Event | Consumer | Action |
|-------|----------|--------|
| `XPAwarded` | Analytics Engine | Update XP statistics |
| `XPAwarded` | Notification Engine | Check for level-up notifications |
| `LevelUp` | Notification Engine | Send level-up notification |
| `AchievementUnlocked` | Notification Engine | Send achievement notification |
| `AchievementUnlocked` | Analytics Engine | Record achievement statistics |

## Integrations

### Task Engine

- Publishes `TaskCompletedEvent`, `TaskFailedEvent`, `TaskCancelledEvent`, `TaskStreakExtendedEvent`
- XP Engine receives events asynchronously
- XP calculated based on task priority, difficulty, execution type
- Different execution types may have different XP values:
  - BOOLEAN: Standard XP
  - CHECKLIST: XP per completed item
  - TIMER: XP based on focus session duration
  - COUNT: XP based on count achieved
  - PROGRESS: XP based on percentage completed
  - HABIT: XP based on streak maintenance
  - APPROVAL: XP only on approval
  - CUSTOM: XP defined by custom provider

### Goal Engine

- Publishes `GoalCompletedEvent`
- XP Engine receives events asynchronously
- XP calculated based on goal's `estimated_xp` and difficulty
- The Goal Engine's `estimated_xp` field serves dual purpose:
  1. Goal Engine uses it for XP-based progress calculation
  2. XP Engine uses it as base XP reward for goal completion

### Other Engines

- **Settings Engine**: Stores user preferences for XP display, level notifications
- **Health Engine**: Publishes health events that may trigger XP rewards
- **Analytics Engine**: Consumes XP data for reporting
- **Notification Engine**: Sends level-up and achievement notifications
- **AI Engine**: May suggest XP optimization strategies

No engine may directly query or modify XP tables. All interaction happens through domain events and the XP Service Interface.

## Future Roadmap

### Planned Features

- Prestige system with permanent multipliers
- XP spending (store, cosmetics, boosts)
- Team/clan XP sharing
- Seasonal XP resets with carry-over
- XP challenges and competitions
- AI-powered XP optimization suggestions

### Scalability Enhancements

- Partition `xp_transactions` by `user_id`
- Archive old transactions to cold storage after 2 years
- Implement read replicas for analytics queries
- Add materialized views for common statistics
- Implement Redis caching for XP accounts

### Integration Opportunities

- Calendar sync for time-based bonuses
- Social features (friend XP comparisons)
- Marketplace for XP-spendable items
- Third-party achievement integration
- Blockchain XP verification (future)
