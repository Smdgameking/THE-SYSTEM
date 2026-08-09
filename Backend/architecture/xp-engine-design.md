# XP Engine Design Document

## Version: 1.0.0
## Date: 2026-08-07
## Status: Draft - Pending Approval

---

## 1. Purpose

The XP Engine is the measurement layer of THE SYSTEM. It quantifies verified effort across all engines and converts user actions into measurable progress. While the Goal Engine defines WHAT users want to achieve and the Task Engine defines HOW users reach those goals, the XP Engine measures the value and effort invested.

The XP Engine provides:
- Immutable XP transaction ledger
- Current XP calculation derived from transactions
- Level progression system with carry-over mechanics
- Achievement definitions and progress tracking
- Configurable reward policies and multipliers
- Reward history with full audit trail
- XP analytics and reporting

---

## 2. Responsibilities

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

### NOT Owned by XP Engine

- Tasks (owned by Task Engine)
- Goals (owned by Goal Engine)
- Task completion logic (owned by Task Engine)
- Goal validation (owned by Goal Engine)
- Health data (owned by Health Engine)
- Memory storage (owned by Memory Engine)
- Notifications (owned by Notification Engine)
- AI suggestions (owned by AI Engine)
- Analytics computation (owned by Analytics Engine)

---

## 3. Ownership and Boundaries

### 3.1 Engine Ownership

The XP Engine exclusively owns:
- `xp_accounts` table
- `xp_transactions` table
- `achievement_definitions` table
- `user_achievements` table
- `xp_policies` table
- `reward_history` table
- All business logic related to XP calculation, level progression, achievements, and rewards
- XP transaction validation and anti-cheat logic
- Reward policy evaluation and multiplier application

### 3.2 Cross-Engine Boundaries

Other engines interact with XP ONLY through:
- **Domain Events**: TaskCompletedEvent, GoalCompletedEvent, etc.
- **XP Service Interface**: Published service methods for XP queries
- **Settings Engine**: Stores user preferences for XP display, level notifications
- **Task Engine**: Publishes task events that may trigger XP rewards
- **Goal Engine**: Publishes goal events that may trigger XP rewards
- **Health Engine**: Publishes health events that may trigger XP rewards
- **Analytics Engine**: Consumes XP data for reporting
- **Notification Engine**: Sends level-up and achievement notifications
- **AI Engine**: May suggest XP optimization strategies

No engine may directly query or modify the `xp_accounts`, `xp_transactions`, `achievement_definitions`, `user_achievements`, `xp_policies`, or `reward_history` tables.

---

## 4. Database Design

### 4.1 XP Accounts Table

```sql
CREATE TABLE IF NOT EXISTS xp_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    current_xp INTEGER NOT NULL DEFAULT 0,
    current_level INTEGER NOT NULL DEFAULT 1,
    total_xp_earned INTEGER NOT NULL DEFAULT 0,
    total_xp_spent INTEGER NOT NULL DEFAULT 0,
    lifetime_xp INTEGER NOT NULL DEFAULT 0,
    level_progress DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_xp_accounts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

**Field Justification**:

| Field | Type | Justification |
|-------|------|---------------|
| `id` | UUID | Primary key |
| `user_id` | UUID | FK to users, one account per user |
| `current_xp` | INTEGER | Cached available XP for display and queries |
| `current_level` | INTEGER | Cached current level for display and queries |
| `total_xp_earned` | INTEGER | Lifetime XP earned (never decreases) |
| `total_xp_spent` | INTEGER | Lifetime XP spent (if XP becomes a currency) |
| `lifetime_xp` | INTEGER | total_xp_earned - total_xp_spent |
| `level_progress` | DOUBLE | Percentage progress to next level (0-100) |

### 4.2 XP Transactions Table

```sql
CREATE TABLE IF NOT EXISTS xp_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    transaction_type VARCHAR(50) NOT NULL,
    amount INTEGER NOT NULL,
    balance_after INTEGER NOT NULL,
    source_engine VARCHAR(50) NOT NULL,
    source_id UUID NULL,
    source_type VARCHAR(50) NULL,
    policy_id UUID NULL,
    multiplier_applied DOUBLE PRECISION NULL,
    base_amount INTEGER NULL,
    reason TEXT,
    metadata JSONB NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_xp_transactions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

**Transaction Types**:

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

### 4.3 Achievement Definitions Table

```sql
CREATE TABLE IF NOT EXISTS achievement_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    icon_url VARCHAR(500) NULL,
    requirement_type VARCHAR(50) NOT NULL,
    requirement_value JSONB NOT NULL,
    xp_reward INTEGER NOT NULL DEFAULT 0,
    is_hidden BOOLEAN NOT NULL DEFAULT FALSE,
    is_repeatable BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    deleted_at TIMESTAMP NULL
);
```

**Achievement Categories**:

| Category | Description | Examples |
|----------|-------------|----------|
| `TASK` | Task-based achievements | Complete 100 tasks |
| `GOAL` | Goal-based achievements | Complete 10 goals |
| `STREAK` | Consistency achievements | 30-day streak |
| `SPEED` | Speed achievements | Complete task in under 5 minutes |
| `EXPLORER` | Diversity achievements | Use 5 different task types |
| `MASTER` | Mastery achievements | Reach level 50 |
| `SPECIAL` | Time-limited or event achievements | Holiday challenges |

### 4.4 User Achievements Table

```sql
CREATE TABLE IF NOT EXISTS user_achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    achievement_id UUID NOT NULL,
    current_progress INTEGER NOT NULL DEFAULT 0,
    target_progress INTEGER NOT NULL,
    is_unlocked BOOLEAN NOT NULL DEFAULT FALSE,
    unlocked_at TIMESTAMP NULL,
    progress_metadata JSONB NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_user_achievements_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_achievements_definition FOREIGN KEY (achievement_id) REFERENCES achievement_definitions(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_achievement UNIQUE (user_id, achievement_id)
);
```

### 4.5 XP Policies Table

```sql
CREATE TABLE IF NOT EXISTS xp_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    policy_type VARCHAR(50) NOT NULL,
    base_xp INTEGER NOT NULL,
    multiplier DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    conditions JSONB NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    deleted_at TIMESTAMP NULL
);
```

**Policy Types**:

| Type | Description | Example |
|------|-------------|---------|
| `TASK_COMPLETION` | Base XP for task completion | +10 XP per task |
| `GOAL_COMPLETION` | Base XP for goal completion | +100 XP per goal |
| `DIFFICULTY_MULTIPLIER` | Multiplier based on task/goal difficulty | 2x for HARD tasks |
| `STREAK_MULTIPLIER` | Multiplier based on consecutive completions | 1.5x for 7-day streak |
| `PRIORITY_MULTIPLIER` | Multiplier based on task priority | 1.25x for HIGH priority |
| `BONUS` | Special bonus XP | Weekend double XP |
| `PENALTY` | XP reduction for missed tasks | -5 XP for missed daily |

### 4.6 Reward History Table

```sql
CREATE TABLE IF NOT EXISTS reward_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    reward_type VARCHAR(50) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_id UUID NOT NULL,
    xp_amount INTEGER NOT NULL,
    policy_id UUID NULL,
    multiplier_applied DOUBLE PRECISION NULL,
    base_amount INTEGER NULL,
    awarded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_reward_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### 4.7 Indexes

```sql
-- XP Accounts
CREATE INDEX IF NOT EXISTS idx_xp_accounts_user_id ON xp_accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_xp_accounts_current_level ON xp_accounts(current_level) WHERE deleted_at IS NULL;

-- XP Transactions
CREATE INDEX IF NOT EXISTS idx_xp_transactions_user_id ON xp_transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_xp_transactions_created_at ON xp_transactions(created_at);
CREATE INDEX IF NOT EXISTS idx_xp_transactions_type ON xp_transactions(transaction_type);
CREATE INDEX IF NOT EXISTS idx_xp_transactions_source ON xp_transactions(source_engine, source_id) WHERE source_id IS NOT NULL;

-- Achievement Definitions
CREATE INDEX IF NOT EXISTS idx_achievement_definitions_code ON achievement_definitions(code) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_achievement_definitions_category ON achievement_definitions(category) WHERE deleted_at IS NULL;

-- User Achievements
CREATE INDEX IF NOT EXISTS idx_user_achievements_user_id ON user_achievements(user_id);
CREATE INDEX IF NOT EXISTS idx_user_achievements_achievement_id ON user_achievements(achievement_id);
CREATE INDEX IF NOT EXISTS idx_user_achievements_unlocked ON user_achievements(is_unlocked, unlocked_at) WHERE is_unlocked = TRUE;

-- XP Policies
CREATE INDEX IF NOT EXISTS idx_xp_policies_code ON xp_policies(code) WHERE deleted_at IS NULL AND is_active = TRUE;
CREATE INDEX IF NOT EXISTS idx_xp_policies_type ON xp_policies(policy_type) WHERE deleted_at IS NULL AND is_active = TRUE;

-- Reward History
CREATE INDEX IF NOT EXISTS idx_reward_history_user_id ON reward_history(user_id);
CREATE INDEX IF NOT EXISTS idx_reward_history_source ON reward_history(source_type, source_id);
CREATE INDEX IF NOT EXISTS idx_reward_history_awarded_at ON reward_history(awarded_at);
```

### 4.8 Entity Relationships

```
users (1) ─── (1) xp_accounts
    │
    └── user_id FK with ON DELETE CASCADE

users (1) ─── (N) xp_transactions
    │
    └── user_id FK with ON DELETE CASCADE

users (1) ─── (N) user_achievements
    │
    └── user_id FK with ON DELETE CASCADE

achievement_definitions (1) ─── (N) user_achievements
    │
    └── achievement_id FK with ON DELETE CASCADE

xp_policies (1) ─── (N) xp_transactions
    │
    └── policy_id FK (optional, nullable for manual transactions)

users (1) ─── (N) reward_history
    │
    └── user_id FK with ON DELETE CASCADE
```

---

## 5. XP Transaction Model

### 5.1 Core Principle

**XP is never modified directly.** Every XP change must create an immutable XP Transaction. Current XP is derived from transactions. Never overwrite history.

### 5.2 Transaction Creation Flow

```
1. External event received (e.g., TaskCompletedEvent)
2. XP Engine validates event authenticity
3. XP Engine evaluates applicable XP policies
4. XP Engine calculates final XP amount (base + multipliers)
5. XP Engine creates XP Transaction record
6. XP Engine updates xp_accounts (cached values)
7. XP Engine checks for level up
8. XP Engine checks for achievement progress
9. XP Engine publishes domain events
10. XP Engine returns result to caller
```

### 5.3 Transaction Metadata

Each XP Transaction contains:
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

### 5.4 Transaction Idempotency

To prevent duplicate rewards:
- Each source entity can only trigger one XP transaction per event
- Duplicate detection uses `(source_engine, source_id, source_type)` composite key within a time window
- Idempotency tokens stored in transaction metadata
- Replay of same event results in no-op (returns existing transaction)

### 5.5 Balance Derivation

Current XP is derived from transactions:

```
current_xp = SUM(amount) WHERE user_id = :userId AND deleted_at IS NULL
current_level = calculate_level(current_xp)
level_progress = calculate_progress(current_xp, current_level)
```

The `xp_accounts` table is a **cache** for performance. It is updated atomically with transaction creation.

---

## 6. Level System

### 6.1 Level Design

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

**Formula**: `xp_required_for_level = 100 * level^1.5` (rounded to nearest 25)

### 6.2 Level Properties

- **Maximum Level**: No hard cap in v1.0; formula scales indefinitely
- **Carry-over XP**: When leveling up, excess XP carries to next level
- **Level Up Detection**: Triggered when transaction causes balance to exceed threshold

### 6.3 Level Algorithm

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

### 6.4 Prestige Support (Future)

Prestige resets level but grants permanent multipliers:
- Prestige rank tracked separately from level
- Each prestige adds 10% base XP multiplier
- Prestige achievements unlocked at milestones
- Architecture supports this via `prestige_rank` column in `xp_accounts` (future migration)

---

## 7. Achievements

### 7.1 Achievement Design

Achievements are defined in `achievement_definitions` and tracked per user in `user_achievements`.

### 7.2 Achievement Definition Structure

```json
{
  "type": "counter",
  "target": 100,
  "filters": {
    "task_status": ["COMPLETED"],
    "task_priority": ["HIGH", "CRITICAL"]
  },
  "group_by": "task_id"
}
```

**Requirement Types**:

| Type | Description | Example |
|------|-------------|---------|
| `counter` | Count occurrences | Complete 100 tasks |
| `streak` | Consecutive days | 30-day streak |
| `milestone` | Reach specific value | Earn 10,000 XP |
| `collection` | Collect variety | Use 5 task types |
| `speed` | Complete within time | Task in under 5 minutes |
| `custom` | Complex logic | Complete 10 tasks in one day |

### 7.3 Achievement Progress Tracking

Progress is updated incrementally:
- Each relevant event updates `current_progress`
- Progress never decreases (except for repeatable achievements with reset)
- `is_unlocked` set when `current_progress >= target_progress`
- `unlocked_at` recorded on unlock

### 7.4 Achievement Rewards

When an achievement is unlocked:
- XP reward issued via XP Transaction
- `AchievementUnlocked` event published
- Notification Engine sends notification
- Analytics Engine records achievement statistics

### 7.5 Achievement Categories

Categories organize achievements for display and filtering:
- `TASK`: Task-related achievements
- `GOAL`: Goal-related achievements
- `STREAK`: Consistency achievements
- `SPEED`: Speed-based achievements
- `EXPLORER`: Variety achievements
- `MASTER`: Mastery achievements
- `SPECIAL`: Time-limited or event achievements

### 7.6 Visibility

- `is_hidden`: Achievements not shown until unlocked
- `is_repeatable`: Can be earned multiple times
- `sort_order`: Controls display order

---

## 8. XP Policies

### 8.1 Policy Design

XP Policies are configurable rules that determine how much XP is awarded. The XP Engine owns policies; the Settings Engine stores configuration values.

### 8.2 Policy Structure

```json
{
  "type": "TASK_COMPLETION",
  "base_xp": 10,
  "multiplier": 1.0,
  "conditions": {
    "priority_multipliers": {
      "LOW": 1.0,
      "NORMAL": 1.0,
      "HIGH": 1.5,
      "CRITICAL": 2.0
    },
    "difficulty_multipliers": {
      "EASY": 1.0,
      "NORMAL": 1.25,
      "HARD": 1.5,
      "EXTREME": 2.0
    },
    "streak_bonus": {
      "enabled": true,
      "milestones": [3, 7, 14, 30, 60, 90],
      "multipliers": [1.1, 1.25, 1.5, 2.0, 2.5, 3.0]
    }
  }
}
```

### 8.3 Policy Evaluation Order

When multiple policies apply:
1. Policies evaluated by `priority` (higher priority first)
2. Base XP from primary policy
3. Multipliers applied cumulatively
4. Final amount = base_xp * multiplier1 * multiplier2 * ...

### 8.4 Policy Types

| Type | Scope | Example |
|------|-------|---------|
| `TASK_COMPLETION` | Per-task reward | +10 XP per completed task |
| `GOAL_COMPLETION` | Per-goal reward | +100 XP per completed goal |
| `DIFFICULTY_MULTIPLIER` | Task/goal difficulty | 2x for EXTREME difficulty |
| `STREAK_MULTIPLIER` | Consecutive completions | 1.5x for 7-day streak |
| `PRIORITY_MULTIPLIER` | Task priority | 1.25x for HIGH priority |
| `BONUS` | Special events | Double XP weekend |
| `PENALTY` | Missed deadlines | -5 XP per missed daily task |

### 8.5 Policy Ownership

- **XP Engine**: Owns policy evaluation logic, policy registry, multiplier application
- **Settings Engine**: Stores policy configuration values (enabled/disabled, custom values)
- **Admin**: Can create/modify policies via Settings Engine

---

## 9. Reward Multipliers

### 9.1 Multiplier Types

Multipliers modify base XP rewards:

| Multiplier | Source | Default Value |
|------------|--------|---------------|
| Difficulty | Task/Goal difficulty | 1.0x - 3.0x |
| Priority | Task priority | 1.0x - 2.0x |
| Streak | Consecutive completions | 1.0x - 3.0x |
| Time of Day | Completion time bonus | 1.0x - 1.5x |
| Day of Week | Weekend bonus | 1.0x - 2.0x |
| Event | Special events | 1.0x - 5.0x |
| Prestige | Prestige rank | 1.0x - 2.0x |

### 9.2 Multiplier Application

```
final_xp = base_xp * difficulty_multiplier * priority_multiplier * streak_multiplier * ...
```

All applicable multipliers are applied multiplicatively.

### 9.3 Multiplier Capping

- Maximum combined multiplier: 10.0x
- Prevents exploit via multiplier stacking
- Configurable via XP policy

---

## 10. Reward History

### 10.1 Purpose

Reward history provides a complete audit trail of all XP awarded to a user.

### 10.2 Data Model

The `reward_history` table records:
- `user_id`: Who received the reward
- `reward_type`: Category of reward
- `source_type`: What triggered the reward
- `source_id`: ID of triggering entity
- `xp_amount`: Amount awarded
- `policy_id`: Policy that determined the reward
- `multiplier_applied`: Final multiplier used
- `base_amount`: XP before multipliers
- `awarded_at`: When the reward was given
- `metadata`: Additional context

### 10.3 Relationship to XP Transactions

Every reward creates both:
1. An entry in `xp_transactions` (immutable ledger)
2. An entry in `reward_history` (query-optimized history)

This denormalization enables:
- Fast queries for "recent rewards"
- Efficient analytics on reward patterns
- Complete audit trail in `xp_transactions`

---

## 11. Task Integration

### 11.1 Event Flow

```
Task Engine publishes: TaskCompletedEvent
    │
    ▼
XP Engine receives event asynchronously
    │
    ├── Validate event authenticity
    ├── Check for duplicate (idempotency)
    ├── Evaluate applicable XP policies
    ├── Calculate XP amount (base + multipliers)
    ├── Create XP Transaction
    ├── Update XP Account
    ├── Check level up
    ├── Update achievement progress
    ├── Publish: XPAwardedEvent
    ├── Publish: LevelUpEvent (if applicable)
    └── Publish: AchievementProgressUpdatedEvent (if applicable)
```

### 11.2 Task Event Mapping

| Task Event | XP Action |
|------------|-----------|
| `TaskCompletedEvent` | Award XP based on task priority, difficulty, execution type |
| `TaskFailedEvent` | No XP (or penalty if policy enabled) |
| `TaskCancelledEvent` | No XP |
| `TaskStreakExtendedEvent` | Award streak bonus XP |

### 11.3 XP Calculation for Tasks

```
base_xp = policy.TASK_COMPLETION.base_xp
priority_multiplier = policy.PRIORITY_MULTIPLIER[task.priority]
difficulty_multiplier = policy.DIFFICULTY_MULTIPLIER[task.difficulty]
streak_multiplier = calculate_streak_multiplier(user_id)
final_xp = base_xp * priority_multiplier * difficulty_multiplier * streak_multiplier
```

### 11.4 Execution Type Considerations

Different execution types may have different XP values:
- BOOLEAN: Standard XP
- CHECKLIST: XP per completed item
- TIMER: XP based on focus session duration
- COUNT: XP based on count achieved
- PROGRESS: XP based on percentage completed
- HABIT: XP based on streak maintenance
- APPROVAL: XP only on approval
- CUSTOM: XP defined by custom provider

---

## 12. Goal Integration

### 12.1 Event Flow

```
Goal Engine publishes: GoalCompletedEvent
    │
    ▼
XP Engine receives event asynchronously
    │
    ├── Validate event authenticity
    ├── Check for duplicate
    ├── Evaluate applicable XP policies
    ├── Calculate XP amount (base + multipliers)
    ├── Create XP Transaction
    ├── Update XP Account
    ├── Check level up
    ├── Check achievement unlocks
    └── Publish: XPAwardedEvent
```

### 12.2 Goal XP Calculation

```
base_xp = goal.estimated_xp
difficulty_multiplier = policy.DIFFICULTY_MULTIPLIER[goal.difficulty]
final_xp = base_xp * difficulty_multiplier
```

### 12.3 Progress Tracking

The Goal Engine's `estimated_xp` field serves dual purpose:
1. Goal Engine uses it for XP-based progress calculation
2. XP Engine uses it as base XP reward for goal completion

This creates a clean contract between engines without direct coupling.

---

## 13. Analytics

### 13.1 XP Statistics

The XP Engine computes and caches the following statistics:

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

### 13.2 Analytics Storage

Statistics are computed on-demand and cached:
- User-level statistics cached for 5 minutes
- Global statistics cached for 15 minutes
- Cache invalidated on new XP transaction

### 13.3 Breakdown Categories

| Breakdown | Description |
|-----------|-------------|
| `by_source` | XP by source engine (Task, Goal, etc.) |
| `by_type` | XP by transaction type |
| `by_achievement` | XP by achievement category |
| `by_time` | XP by hour of day, day of week |
| `by_policy` | XP by policy applied |

---

## 14. Anti-Cheat

### 14.1 Duplicate Event Protection

Each event can only trigger one XP transaction:
- Composite key: `(source_engine, source_id, source_type)`
- Time window: 5 minutes for retry tolerance
- Idempotency token in event metadata

### 14.2 Replay Protection

Historical events cannot be replayed to generate duplicate XP:
- Event timestamps validated against transaction timestamps
- Events older than 24 hours require manual review
- Replay attempts logged and flagged

### 14.3 Transaction Idempotency

XP Transactions are immutable:
- Once created, never modified
- Duplicate detection prevents double-awarding
- All state changes create new transactions

### 14.4 Audit Trail

Complete audit trail via `xp_transactions`:
- Every XP change recorded with full context
- Balance after each transaction
- Source engine and entity tracked
- Metadata preserves original event data

### 14.5 Anomaly Detection (Future)

Future enhancements:
- Unusual XP spikes flagged for review
- Velocity checks (max XP per hour)
- Pattern detection for exploit attempts
- Admin override with audit logging

---

## 15. Domain Events

### 15.1 Event Catalog

The XP Engine publishes the following domain events:

| Event | Trigger | Payload |
|-------|---------|---------|
| `XPAwarded` | XP transaction created | xp_amount, user_id, source_type, source_id, transaction_type |
| `XPRemoved` | XP penalty or reversal | xp_amount, user_id, reason |
| `LevelUp` | User levels up | user_id, old_level, new_level, xp_required |
| `AchievementUnlocked` | Achievement unlocked | user_id, achievement_id, achievement_code, xp_reward |
| `AchievementProgressUpdated` | Achievement progress changed | user_id, achievement_id, current_progress, target_progress |
| `PolicyChanged` | XP policy modified | policy_id, policy_code, changes |
| `XPAdjusted` | Admin manual adjustment | user_id, amount, reason, admin_id |

### 15.2 Event Schema

All events follow the standard envelope:

```json
{
  "event_id": "uuid",
  "event_type": "XPAwarded",
  "event_version": "1.0",
  "timestamp": "2026-08-07T10:00:00Z",
  "source": "xp-engine",
  "payload": {
    "user_id": "uuid",
    "xp_amount": 10,
    "source_type": "TASK",
    "source_id": "uuid",
    "transaction_type": "TASK_COMPLETION"
  },
  "metadata": {
    "correlation_id": "uuid",
    "causation_id": "uuid"
  }
}
```

### 15.3 Event Consumers

| Event | Consumer | Action |
|-------|----------|--------|
| `XPAwarded` | Analytics Engine | Update XP statistics |
| `XPAwarded` | Notification Engine | Check for level-up notifications |
| `LevelUp` | Notification Engine | Send level-up notification |
| `AchievementUnlocked` | Notification Engine | Send achievement notification |
| `AchievementUnlocked` | Analytics Engine | Record achievement statistics |

---

## 16. Scalability

### 16.1 Transaction Volume

Designed for millions of transactions without schema changes:
- `xp_transactions` table partitioned by `user_id` (future)
- Efficient indexes on `user_id` and `created_at`
- Read replicas for analytics queries
- Materialized views for statistics (future)

### 16.2 Caching Strategy

- `xp_accounts` cached in Redis (future)
- User XP queries served from cache
- Cache invalidation on new transaction
- Statistics computed from cache with periodic refresh

### 16.3 Read Optimization

- `xp_accounts` serves 95% of reads
- `reward_history` optimized for recent queries
- `xp_transactions` used for audit and detailed queries
- Pagination required for transaction lists

### 16.4 Write Optimization

- Transaction creation is single INSERT
- `xp_accounts` update in same transaction
- Batch operations for achievement progress updates
- Async event publishing to avoid blocking

---

## 17. Security and Authorization

### 17.1 Ownership Model

- Every XP transaction belongs to exactly one user (`user_id`)
- Users can only view their own XP data
- Admins can view any user's XP data
- XP adjustments require admin role

### 17.2 Authorization Rules

| Operation | Required Permission |
|-----------|---------------------|
| View own XP | Authenticated user |
| View own transactions | Authenticated user |
| View own achievements | Authenticated user |
| Adjust XP (admin) | Admin role |
| Create XP policy | Admin role |
| Modify XP policy | Admin role |

### 17.3 Data Isolation

- All queries filter by `user_id`
- Admin endpoints require elevated permissions
- Transaction history is immutable (no updates or deletes)
- Soft delete only for policy and definition tables

---

## 18. API Design

### 18.1 REST Endpoints

```
XP
GET    /api/v1/xp/account              Get current XP account
GET    /api/v1/xp/transactions         List XP transactions
GET    /api/v1/xp/transactions/:id     Get transaction by ID
GET    /api/v1/xp/statistics           Get XP statistics
GET    /api/v1/xp/leaderboard          Get XP leaderboard

ACHIEVEMENTS
GET    /api/v1/achievements            List all achievements
GET    /api/v1/achievements/:id        Get achievement definition
GET    /api/v1/achievements/user       List user achievements
GET    /api/v1/achievements/user/:id   Get user achievement progress
POST   /api/v1/achievements/check      Check for new achievements (internal)

POLICIES
GET    /api/v1/xp/policies             List XP policies
GET    /api/v1/xp/policies/:id         Get XP policy
POST   /api/v1/xp/policies             Create XP policy (admin)
PATCH  /api/v1/xp/policies/:id         Update XP policy (admin)
DELETE /api/v1/xp/policies/:id         Delete XP policy (admin)
```

### 18.2 Query Parameters

```yaml
GET /api/v1/xp/transactions:
  query_params:
    - transaction_type: String[]        # Filter by type
    - start_date: ISO8601               # Start of date range
    - end_date: ISO8601                 # End of date range
    - source_type: String               # Filter by source engine
    - sort_by: String                   # created_at, amount
    - sort_order: String                # asc, desc
    - page: Integer                     # Page number
    - limit: Integer                    # Items per page

GET /api/v1/xp/leaderboard:
  query_params:
    - period: String                    # today, week, month, all
    - limit: Integer                    # Max entries (default 100)
```

### 18.3 Error Codes

| HTTP Status | Code | Description |
|-------------|------|-------------|
| 400 | `INVALID_TRANSACTION` | Transaction validation failed |
| 400 | `DUPLICATE_TRANSACTION` | Transaction already exists |
| 400 | `INVALID_AMOUNT` | XP amount out of range |
| 404 | `XP_ACCOUNT_NOT_FOUND` | User has no XP account |
| 404 | `ACHIEVEMENT_NOT_FOUND` | Achievement does not exist |
| 404 | `TRANSACTION_NOT_FOUND` | Transaction does not exist |
| 409 | `LEVEL_LOCKED` | Cannot perform action at current level |
| 422 | `VALIDATION_ERROR` | General validation failure |

---

## 19. Performance Considerations

### 19.1 Query Optimization

- `xp_accounts` serves all balance queries (single row lookup)
- `xp_transactions` indexed by `(user_id, created_at)` for history queries
- `reward_history` indexed by `(user_id, awarded_at)` for recent rewards
- Pagination required for transaction lists

### 19.2 N+1 Query Prevention

- Achievement progress loaded in batch
- Transaction aggregates pre-computed
- Leaderboard computed via materialized view (future)

### 19.3 Scalability

- `xp_transactions` partitioned by `user_id` for very large deployments
- Read replicas for analytics queries
- Event publishing uses async message queue
- Cache invalidation via event-driven updates

---

## 20. Monitoring and Observability

### 20.1 Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `xp.transaction.count` | Counter | Total XP transactions created |
| `xp.transaction.amount` | Histogram | XP amount per transaction |
| `xp.level.up.count` | Counter | Total level-ups |
| `xp.achievement.unlock.count` | Counter | Total achievements unlocked |
| `xp.policy.evaluate.duration` | Histogram | Time to evaluate XP policy |
| `xp.transaction.create.duration` | Histogram | Time to create transaction |

### 20.2 Logging

- All transaction creations logged with full context
- Policy evaluation results logged
- Level-up events logged
- Achievement unlock events logged
- Error conditions include full context for debugging

### 20.3 Alerting

- Alert if transaction creation rate drops >50% (possible service issue)
- Alert if duplicate transaction rate spikes (possible exploit)
- Alert if XP balance anomalies detected

---

## 21. Testing Strategy

### 21.1 Unit Tests

- Test each level calculation boundary
- Test each achievement progress update
- Test each XP policy evaluation
- Test multiplier application
- Test anti-cheat duplicate detection
- Test transaction idempotency

### 21.2 Integration Tests

- Test full XP flow from event to transaction
- Test level-up detection and event publishing
- Test achievement unlock flow
- Test policy evaluation with multiple policies
- Test concurrent transaction creation
- Test anti-cheat under load

### 21.3 Contract Tests

- Verify API response schemas against OpenAPI spec
- Verify event schemas against event catalog
- Verify database schema matches design document

### 21.4 Performance Tests

- Load test transaction creation (target: 10,000 transactions/second)
- Load test XP account queries (target: 50,000 requests/second)
- Load test leaderboard generation (target: 100 requests/second)

---

## 22. Future Considerations

### 22.1 Planned Features

- Prestige system with permanent multipliers
- XP spending (store, cosmetics, boosts)
- Team/clan XP sharing
- Seasonal XP resets with carry-over
- XP challenges and competitions
- AI-powered XP optimization suggestions

### 22.2 Scalability Enhancements

- Partition `xp_transactions` by `user_id`
- Archive old transactions to cold storage after 2 years
- Implement read replicas for analytics queries
- Add materialized views for common statistics
- Implement Redis caching for XP accounts

### 22.3 Integration Opportunities

- Calendar sync for time-based bonuses
- Social features (friend XP comparisons)
- Marketplace for XP-spendable items
- Third-party achievement integration
- Blockchain XP verification (future)

---

## Streak Engine — Resolved Rules

This section resolves all previously undefined streak rules and establishes the authoritative behavior for Streak Engine implementation.

### 1. Streak Ownership

* THE SYSTEM uses ONE unified, user-wide streak.
* Tasks and goals both contribute to the same streak.
* HABIT task `execution_state.streak` remains a separate per-task habit streak and must not be confused with the global user streak.

### 2. Streak Freeze

* No streak-freeze functionality in v1.
* Do not create database fields, APIs, or policies for streak freezes yet.

### 3. Manual Restoration

* Users cannot manually restore their streak.
* Administrators may restore a streak in a future/admin workflow.
* Any future restoration must be auditable.
* Do not implement the admin restoration workflow yet.

### 4. Timezone Changes

* `UserProfile.timezone` is the source of truth for determining activity dates.
* Historical streak activity is NOT rewritten when a user changes timezone.
* Existing historical records retain their originally calculated `activity_date`.
* Future completion events use the user's new timezone.
* If timezone is null or invalid, use UTC as the fallback.

### 5. Minimum Daily Requirement

* A calendar day is considered active when the user completes at least ONE qualifying activity.
* A qualifying activity is:
  * Task transitioning to COMPLETED
  * Goal transitioning to COMPLETED
* Multiple completions on the same day do not increase the streak by multiple days.
* Failed, cancelled, deleted, progress-only, or manual XP activity does not qualify.

### 6. Streak Break Behavior

* A day with no qualifying activity is an inactive day.
* An inactive day breaks the consecutive streak.
* Failed or cancelled tasks do not directly reset the streak; they simply do not contribute activity.
* If another qualifying task or goal is completed that day, the day remains active.

### 7. Recurring / HABIT Task Behavior

* Skipping a recurring/HABIT task does not have special global-streak behavior.
* If the user completes another qualifying task or goal that day, the global streak continues.
* If there is no qualifying activity that day, the global streak breaks.
* Per-task HABIT streak behavior remains owned by the Task Engine.

### 8. Streak History Source

* Streak history is event-driven.
* The Streak Engine maintains dedicated streak activity/history data.
* Do NOT derive the authoritative streak state by scanning `xp_transactions`.
* XP transactions and streak history are separate concerns.

### 9. Day Boundary

* Streaks use strict calendar days.
* The day boundary is 00:00:00 in the user's configured timezone.
* Do not introduce a grace period in v1.
* Convert the completion event's `occurredAt` timestamp into the user's timezone before determining `activity_date`.

### 10. Maximum Streak

* There is NO maximum streak length.
* `current_streak` and `longest_streak` can grow indefinitely within the database integer limits.
* XP multiplier remains independently capped at the existing 10.0x policy cap.

### Core Streak Semantics

* First qualifying completion ever → current streak becomes 1.
* Consecutive qualifying calendar days increment the streak by exactly 1 per active day.
* A gap of one or more inactive calendar days breaks the current streak.
* `longest_streak` records the historical maximum.
* A new activity after a broken streak starts a new streak at 1.
* Multiple task/goal completions on the same calendar day increase activity counts but do not increase the streak by more than one day.
* Out-of-order events must use their original `occurredAt` timestamp rather than processing time. The exact recalculation behavior for out-of-order events is part of the future implementation contract.

### Architectural Distinction

**GLOBAL USER STREAK**

* Owned by XP / Streak Engine.
* One streak per user.
* Task and goal completions contribute.

**HABIT TASK STREAK**

* Owned by Task Engine.
* Per task.
* Stored in task `execution_state`.
* Used for habit/task-specific behavior.
* Must not be treated as the user's global streak.

---

## Appendix A: Glossary

| Term | Definition |
|------|------------|
| XP | Experience Points, the unit of measurable effort |
| Transaction | Immutable record of an XP change |
| Level | Progression tier based on total XP |
| Achievement | Goal-based reward for completing specific actions |
| Policy | Configurable rule that determines XP rewards |
| Multiplier | Factor that modifies base XP |
| Reward | XP awarded for completing an action |
| Ledger | Complete history of all XP transactions |

---

## Appendix B: References

- Backend Constitution: `architecture/backend-constitution.md`
- Database Constitution: `architecture/database-constitution.md`
- API Constitution: `architecture/api-constitution.md`
- Module Constitution: `architecture/module-constitution.md`
- Task Engine Design: `architecture/task-engine-design.md`
- Goal Engine Design: `architecture/goal-engine-design.md`
- ADR-0006: Cross-Engine Progress Calculation Pattern
- ADR-0007: Task Execution Provider Pattern
- PostgreSQL JSONB Documentation
- Domain-Driven Design by Eric Evans

---

*This document is part of THE SYSTEM Backend Architecture. All rules from the Backend Constitution apply.*
