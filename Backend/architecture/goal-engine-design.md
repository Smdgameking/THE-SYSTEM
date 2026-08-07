# Goal Engine Design Document

## Version: 1.0.0
## Date: 2026-08-07
## Status: Draft - Pending Approval

---

## 1. Purpose

The Goal Engine is the heart of THE SYSTEM. It is the single source of truth for all user objectives, achievements, projects, and aspirations. Every long-term achievement, project, mission, learning objective, and personal objective originates here.

The Goal Engine provides:
- Goal lifecycle management (creation, tracking, completion, archiving)
- Progress tracking and calculation
- Milestone management
- Goal categorization and organization
- Integration points for other engines to contribute to goals

---

## 2. Responsibilities

### Owned by Goal Engine

- **Goal Entities**: Core goal data model with lifecycle state
- **Goal Lifecycle**: State machine for goal status transitions
- **Goal Progress**: Automatic and manual progress tracking
- **Goal Status**: DRAFT, ACTIVE, PAUSED, COMPLETED, FAILED, ARCHIVED
- **Goal Priority**: LOW, NORMAL, HIGH, CRITICAL
- **Goal Difficulty**: EASY, NORMAL, HARD, EXTREME
- **Goal Categories**: User-defined categorization
- **Goal Deadlines**: Target dates and deadline management
- **Goal Milestones**: Sub-goals and checkpoints within goals
- **Goal Statistics**: Completion rates, time-to-complete, streak tracking
- **Goal Completion Strategy**: Flexible completion criteria
- **Goal Tags**: Flexible tagging system
- **Goal Visibility**: Public/private goal settings

### NOT Owned by Goal Engine

- Tasks (owned by Task Engine)
- XP calculation and awards (owned by XP Engine)
- Memory storage (owned by Memory Engine)
- Analytics computation (owned by Analytics Engine)
- Notifications (owned by Notification Engine)
- AI suggestions (owned by AI Engine)
- User authentication (owned by Auth Engine)
- User profiles (owned by User Engine)
- Settings (owned by Settings Engine)

---

## 3. Ownership and Boundaries

### 3.1 Engine Ownership

The Goal Engine exclusively owns:
- `goals` table
- `goal_milestones` table
- `goal_tags` table (or JSONB column)
- All business logic related to goals
- Goal state transitions
- Progress calculation logic
- Milestone management

### 3.2 Cross-Engine Boundaries

Other engines interact with goals ONLY through:
- **Goal Service Interface**: Published service methods
- **Domain Events**: GoalCreated, GoalCompleted, etc.
- **Settings Engine**: For goal-related user preferences
- **Task Engine**: Contributes task completion to goal progress
- **XP Engine**: Awards XP upon goal completion
- **Analytics Engine**: Consumes goal data for reporting
- **Notification Engine**: Sends reminders for goal deadlines
- **AI Engine**: Suggests goals based on user patterns

No engine may directly query or modify the `goals` or `goal_milestones` tables.

---

## 4. Database Design

### 4.1 Goals Table

```sql
CREATE TABLE IF NOT EXISTS goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    difficulty VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    estimated_xp INTEGER NOT NULL DEFAULT 0,
    current_progress INTEGER NOT NULL DEFAULT 0,
    completion_percentage DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    target_date TIMESTAMP NULL,
    completed_date TIMESTAMP NULL,
    archived_date TIMESTAMP NULL,
    completion_strategy VARCHAR(50),
    tags JSONB NULL,
    custom_metadata JSONB NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_goals_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### 4.2 Goal Milestones Table

```sql
CREATE TABLE IF NOT EXISTS goal_milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_date TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_goal_milestones_goal FOREIGN KEY (goal_id) REFERENCES goals(id) ON DELETE CASCADE
);
```

### 4.3 Indexes

```sql
-- User goals
CREATE INDEX IF NOT EXISTS idx_goals_user_id ON goals(user_id);
CREATE INDEX IF NOT EXISTS idx_goals_user_status ON goals(user_id, status) WHERE deleted_at IS NULL;

-- Filtering
CREATE INDEX IF NOT EXISTS idx_goals_status ON goals(status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_goals_priority ON goals(priority) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_goals_category ON goals(category) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_goals_target_date ON goals(target_date) WHERE deleted_at IS NULL AND target_date IS NOT NULL;

-- Milestones
CREATE INDEX IF NOT EXISTS idx_goal_milestones_goal_id ON goal_milestones(goal_id);
CREATE INDEX IF NOT EXISTS idx_goal_milestones_display_order ON goal_milestones(goal_id, display_order);

-- Soft delete
CREATE INDEX IF NOT EXISTS idx_goals_deleted_at ON goals(deleted_at);
CREATE INDEX IF NOT EXISTS idx_goal_milestones_deleted_at ON goal_milestones(deleted_at);
```

### 4.4 Unique Constraints

```sql
-- No unique constraint on goal titles within a user (users may have similar goals)
-- Milestones are unique per goal by display_order
CREATE UNIQUE INDEX IF NOT EXISTS uq_goal_milestones_goal_display_order 
    ON goal_milestones(goal_id, display_order) 
    WHERE deleted_at IS NULL;
```

### 4.5 Entity Relationships

```
users (1) ─── (N) goals
    │
    └── user_id FK with ON DELETE CASCADE

goals (1) ─── (N) goal_milestones
    │
    └── goal_id FK with ON DELETE CASCADE
```

---

## 5. Goal Model

### 5.1 Field Justification

| Field | Type | Justification |
|-------|------|---------------|
| `id` | UUID | Primary key, distributed-system friendly |
| `user_id` | UUID | FK to users, every goal belongs to a user |
| `title` | VARCHAR(255) | Required, concise goal name |
| `description` | TEXT | Optional, detailed goal description |
| `category` | VARCHAR(50) | User-defined category for organization |
| `priority` | VARCHAR(20) | LOW, NORMAL, HIGH, CRITICAL for prioritization |
| `difficulty` | VARCHAR(20) | EASY, NORMAL, HARD, EXTREME for XP calculation |
| `status` | VARCHAR(20) | DRAFT, ACTIVE, PAUSED, COMPLETED, FAILED, ARCHIVED |
| `visibility` | VARCHAR(20) | PRIVATE, FRIENDS, PUBLIC for sharing |
| `estimated_xp` | INTEGER | XP reward upon completion |
| `current_progress` | INTEGER | Manual or automatic progress value |
| `completion_percentage` | DOUBLE | Calculated percentage (0-100) |
| `target_date` | TIMESTAMP | Optional deadline |
| `completed_date` | TIMESTAMP | Set when status becomes COMPLETED |
| `archived_date` | TIMESTAMP | Set when status becomes ARCHIVED |
| `completion_strategy` | VARCHAR(50) | MANUAL, TASK_BASED, XP_BASED, MILESTONE_BASED, PERCENTAGE, CUSTOM |
| `tags` | JSONB | Flexible tagging for filtering |
| `custom_metadata` | JSONB | Engine-specific extensions without schema changes |
| `created_at` | TIMESTAMP | Audit field from BaseEntity |
| `updated_at` | TIMESTAMP | Audit field from BaseEntity |
| `created_by` | UUID | Audit field from BaseEntity |
| `updated_by` | UUID | Audit field from BaseEntity |
| `deleted_at` | TIMESTAMP | Soft delete from BaseEntity |

### 5.2 Field Decisions

- **`current_progress` vs `completion_percentage`**: Both exist for flexibility. `current_progress` is an integer value (e.g., 7 of 10 tasks), while `completion_percentage` is the calculated percentage. Different completion strategies use different metrics.
- **`custom_metadata`**: Allows engines to store engine-specific data without schema changes. For example, AI Engine can store suggestion metadata, Analytics Engine can store tracking parameters.
- **`tags`**: JSONB array for flexible categorization without requiring a separate tags table.
- **`completion_strategy`**: Determines how the goal is considered complete. Allows users to choose their preferred completion model.

---

## 6. Goal Types

### 6.1 Supported Types

| Type | Description | v1.0 |
|------|-------------|------|
| `LONG_TERM` | Multi-month or year-long objectives | Yes |
| `SHORT_TERM` | Daily, weekly, monthly goals | Yes |
| `PROJECT` | Complex goals with multiple deliverables | Yes |
| `LEARNING` | Skill acquisition, courses, certifications | Yes |
| `HEALTH` | Fitness, nutrition, medical goals | Yes |
| `CAREER` | Professional development, job search | Yes |
| `HABIT` | Recurring behavioral goals | Yes |
| `CUSTOM` | User-defined type | Yes |

### 6.2 Implementation

Goal types are stored as a `VARCHAR(50)` column. No separate table is needed in v1.0. Types are predefined in an enum for validation, but users can also create custom types.

**Decision**: Include all goal types in v1.0 because:
- They are core to THE SYSTEM's purpose as a personal operating system
- Simple to implement as string enums
- Users expect diverse goal categorization
- No additional schema complexity

---

## 7. Goal Status Lifecycle

### 7.1 Status Values

| Status | Description |
|--------|-------------|
| `DRAFT` | Goal is being planned, not yet active |
| `ACTIVE` | Goal is in progress |
| `PAUSED` | Goal is temporarily suspended |
| `COMPLETED` | Goal has been successfully achieved |
| `FAILED` | Goal was not achieved (optional, for tracking) |
| `ARCHIVED` | Goal is no longer active, kept for history |

### 7.2 State Transition Diagram

```
    ┌─────────────────────────────────────────────────────────────┐
    │                                                             │
    ▼                                                             │
┌───────┐    start    ┌───────┐    pause    ┌───────┐    archive  │
│ DRAFT │ ──────────▶ │ ACTIVE │ ──────────▶ │ PAUSED │ ──────────┐ │
└───────┘             └───────┘             └───────┘           │ │
     ▲                    │    ▲                  │              │ │
     │                    │    │                  │              │ │
     │                    │    │    complete      │    resume    │ │
     │                    │    └──────────────────┘              │ │
     │                    │                                      │ │
     │                    │    fail                              │ │
     │                    └──────────────────────────────────────┘ │
     │                                                             │
     └─────────────────────────────────────────────────────────────┘
```

### 7.3 Valid Transitions

| From | To | Trigger |
|------|----|---------|
| DRAFT | ACTIVE | User starts goal |
| DRAFT | ARCHIVED | User discards goal |
| ACTIVE | PAUSED | User pauses goal |
| ACTIVE | COMPLETED | Goal completion criteria met |
| ACTIVE | FAILED | User marks goal as failed |
| ACTIVE | ARCHIVED | User archives active goal |
| PAUSED | ACTIVE | User resumes goal |
| PAUSED | ARCHIVED | User archives paused goal |
| COMPLETED | ARCHIVED | User archives completed goal |
| FAILED | ACTIVE | User retries failed goal |
| FAILED | ARCHIVED | User archives failed goal |

### 7.4 Transition Rules

- DRAFT → ACTIVE: Requires title at minimum
- ACTIVE → COMPLETED: Requires `completion_percentage >= 100` OR manual completion
- ACTIVE → FAILED: Allowed anytime, requires reason in `custom_metadata`
- Any terminal state (COMPLETED, FAILED, ARCHIVED) → ACTIVE: Requires resetting progress
- ARCHIVED: Final state, cannot return to ACTIVE without creating new goal

---

## 8. Priority System

### 8.1 Priority Levels

| Priority | Value | Description |
|----------|-------|-------------|
| `LOW` | 1 | Nice to have, no urgency |
| `NORMAL` | 2 | Standard priority (default) |
| `HIGH` | 3 | Important, should focus on soon |
| `CRITICAL` | 4 | Essential, must complete |

### 8.2 Rationale

- Simple 4-level scale matches user mental models
- Numeric values enable sorting and filtering
- `NORMAL` is the sensible default to avoid decision fatigue
- `CRITICAL` is reserved for essential goals only

---

## 9. Difficulty System

### 9.1 Difficulty Levels

| Difficulty | XP Multiplier | Description |
|------------|---------------|-------------|
| `EASY` | 1.0x | Simple, straightforward goals |
| `NORMAL` | 1.5x | Moderate effort required |
| `HARD` | 2.0x | Significant effort and time |
| `EXTREME` | 3.0x | Major life achievement |

### 9.2 Rationale

- Difficulty affects XP reward calculation (XP Engine)
- Provides gamification balance
- Helps users set appropriate expectations
- Enables analytics on goal completion rates by difficulty
- **Decision**: Include in v1.0 because difficulty is core to THE SYSTEM's gamification layer

---

## 10. Progress System

### 10.1 Progress Design

Progress is **hybrid**: primarily automatic with manual override capability.

### 10.2 Automatic Progress

Automatic progress is calculated by the Goal Engine based on contributions from other engines:

| Source | Calculation |
|--------|-------------|
| Task Engine | `(completed_tasks / total_tasks) * 100` |
| Milestones | `(completed_milestones / total_milestones) * 100` |
| XP Engine | `(current_xp / estimated_xp) * 100` (if applicable) |

### 10.3 Manual Progress

Users can manually set progress when:
- No tasks are linked yet
- Progress is not task-based (e.g., "run a marathon")
- User wants to override automatic calculation

### 10.4 Progress Resolution

```
IF completion_strategy == 'TASK_BASED':
    progress = calculate_from_tasks()
ELIF completion_strategy == 'MILESTONE_BASED':
    progress = calculate_from_milestones()
ELIF completion_strategy == 'XP_BASED':
    progress = calculate_from_xp()
ELIF completion_strategy == 'PERCENTAGE':
    progress = manual_percentage
ELSE (MANUAL):
    progress = manual_progress
```

### 10.5 Progress Events

When progress changes:
1. Goal Engine updates `current_progress` and `completion_percentage`
2. Goal Engine publishes `GoalProgressUpdatedEvent`
3. XP Engine listens and updates XP tracking
4. Analytics Engine listens and updates statistics
5. Notification Engine checks if milestone thresholds are crossed

---

## 11. Milestones

### 11.1 Milestone Ownership

Milestones are owned and managed exclusively by the Goal Engine.

### 11.2 Milestone Model

```sql
CREATE TABLE IF NOT EXISTS goal_milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_date TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP NULL
);
```

### 11.3 Milestone Fields

| Field | Justification |
|-------|---------------|
| `id` | UUID PK |
| `goal_id` | FK to parent goal, CASCADE delete |
| `title` | Required, concise milestone name |
| `description` | Optional details |
| `display_order` | Explicit ordering (not relying on creation order) |
| `is_completed` | Boolean completion flag |
| `completed_date` | Timestamp of completion |
| `deleted_at` | Soft delete support |

### 11.4 Milestone Ordering

- `display_order` is explicit integer (0, 1, 2, ...)
- When inserting between existing milestones, reorder subsequent milestones
- Milestones are always displayed sorted by `display_order`

### 11.5 Milestone Completion

- Milestones can be completed independently
- Completing all milestones can trigger goal completion (if strategy is `MILESTONE_BASED`)
- Milestone completion publishes `MilestoneCompletedEvent`
- Goal Engine recalculates progress when milestones change

### 11.6 Future Task Integration

Future integration with Task Engine:
- Milestones can have associated tasks (via Task Engine)
- Completing all tasks for a milestone can auto-complete the milestone
- This requires Task Engine to expose milestone-task relationship API
- **Decision**: Do not implement in v1.0; design for future integration

---

## 12. Completion Strategy

### 12.1 Strategy Types

| Strategy | Description | Calculation |
|----------|-------------|-------------|
| `MANUAL` | User marks goal as complete | N/A |
| `TASK_BASED` | Complete X of Y tasks | `completed_tasks / total_tasks * 100` |
| `XP_BASED` | Reach target XP | `current_xp / estimated_xp * 100` |
| `MILESTONE_BASED` | Complete all milestones | `completed_milestones / total_milestones * 100` |
| `PERCENTAGE` | Manual percentage target | User-specified percentage |
| `CUSTOM` | User-defined condition | Evaluated by custom logic |

### 12.2 Strategy Architecture

```java
public interface CompletionStrategy {
    boolean isComplete(Goal goal);
    double calculateProgress(Goal goal);
}

public class TaskBasedStrategy implements CompletionStrategy {
    public boolean isComplete(Goal goal) {
        return goal.getCurrentProgress() >= 100;
    }
    public double calculateProgress(Goal goal) {
        // Query Task Engine for linked tasks
        // Return completion percentage
    }
}
```

### 12.3 Strategy Selection

- Default: `MANUAL` for new goals
- User can change strategy at any time
- Changing strategy recalculates progress
- Some strategies require additional setup (e.g., TASK_BASED requires linking tasks)

### 12.4 Custom Strategy

- `custom_metadata` stores strategy-specific configuration
- Example: `{"requiredTasks": 10, "requiredXP": 500, "customCondition": "..."}`
- Custom logic is evaluated by the Goal Engine or delegated to AI Engine

---

## 13. Engine Communication

### 13.1 Task Engine

**Direction**: Task Engine → Goal Engine

**Mechanism**: 
- Task Engine publishes `TaskCompletedEvent`
- Goal Engine subscribes and updates goal progress
- Task Engine exposes `getTasksForGoal(goalId)` for progress calculation

**Data Flow**:
```
Task Completed
    ↓
TaskCompletedEvent published
    ↓
Goal Engine receives event
    ↓
Goal Engine queries Task Engine for task count
    ↓
Goal Engine updates goal progress
    ↓
GoalProgressUpdatedEvent published
```

### 13.2 XP Engine

**Direction**: Goal Engine → XP Engine

**Mechanism**:
- Goal Engine publishes `GoalCompletedEvent`
- XP Engine awards XP based on `estimated_xp` and `difficulty`
- XP Engine exposes `awardXP(userId, amount, source)`

**Data Flow**:
```
Goal Completed
    ↓
GoalCompletedEvent published
    ↓
XP Engine receives event
    ↓
XP Engine calculates reward (estimated_xp * difficulty_multiplier)
    ↓
XP Engine awards XP to user
```

### 13.3 Analytics Engine

**Direction**: Goal Engine → Analytics Engine

**Mechanism**:
- Goal Engine publishes goal lifecycle events
- Analytics Engine consumes events for reporting
- Analytics Engine queries Goal Engine for historical data

**Events Consumed**:
- GoalCreatedEvent
- GoalStartedEvent
- GoalCompletedEvent
- GoalFailedEvent
- GoalArchivedEvent

### 13.4 Notification Engine

**Direction**: Goal Engine → Notification Engine

**Mechanism**:
- Goal Engine publishes deadline reminders
- Notification Engine sends notifications based on user preferences (from Settings Engine)

**Events**:
- GoalDeadlineApproachingEvent (7 days, 3 days, 1 day before)
- GoalStalledEvent (no progress for X days)
- GoalCompletedEvent (celebration notification)

### 13.5 AI Engine

**Direction**: AI Engine → Goal Engine (suggestions), Goal Engine → AI Engine (context)

**Mechanism**:
- AI Engine analyzes user patterns and suggests goals
- AI Engine publishes `GoalSuggestedEvent`
- Goal Engine exposes `createGoalFromSuggestion(suggestion)` API
- Goal Engine provides context to AI Engine for better suggestions

### 13.6 Settings Engine

**Direction**: Settings Engine → Goal Engine (preferences)

**Mechanism**:
- Goal Engine reads user preferences from Settings Engine
- Settings: default difficulty, reminder preferences, visibility defaults, completion strategy defaults

---

## 14. Domain Events

### 14.1 Event List

| Event | When Published | Consumers |
|-------|----------------|-----------|
| `GoalCreatedEvent` | Goal is created | Analytics, Notification (welcome) |
| `GoalStartedEvent` | Goal status changes DRAFT → ACTIVE | Analytics, Notification |
| `GoalPausedEvent` | Goal status changes ACTIVE → PAUSED | Analytics |
| `GoalResumedEvent` | Goal status changes PAUSED → ACTIVE | Analytics, Notification |
| `GoalCompletedEvent` | Goal status changes to COMPLETED | XP, Analytics, Notification (celebration) |
| `GoalFailedEvent` | Goal status changes to FAILED | Analytics, Notification (encouragement) |
| `GoalArchivedEvent` | Goal status changes to ARCHIVED | Analytics |
| `GoalProgressUpdatedEvent` | Goal progress changes | XP, Analytics, Notification (milestone) |
| `MilestoneCreatedEvent` | Milestone is added to goal | Notification |
| `MilestoneCompletedEvent` | Milestone is completed | XP, Analytics, Notification |
| `GoalDeletedEvent` | Goal is soft-deleted | Analytics |

### 14.2 Event Structure

```java
public record GoalCreatedEvent(
    UUID goalId,
    UUID userId,
    String title,
    String category,
    GoalType type,
    Instant occurredAt
) implements DomainEvent {}

public record GoalProgressUpdatedEvent(
    UUID goalId,
    UUID userId,
    int oldProgress,
    int newProgress,
    double oldPercentage,
    double newPercentage,
    String source, // "TASK", "MILESTONE", "MANUAL", "XP"
    Instant occurredAt
) implements DomainEvent {}
```

---

## 15. Public API Design

### 15.1 Service Interface

```java
public interface GoalService {
    
    // Goal CRUD
    GoalResponse createGoal(UUID userId, CreateGoalRequest request);
    GoalResponse getGoal(UUID userId, UUID goalId);
    List<GoalResponse> getGoals(UUID userId, GoalFilter filter);
    GoalResponse updateGoal(UUID userId, UUID goalId, UpdateGoalRequest request);
    void deleteGoal(UUID userId, UUID goalId);
    
    // Goal Lifecycle
    GoalResponse startGoal(UUID userId, UUID goalId);
    GoalResponse pauseGoal(UUID userId, UUID goalId);
    GoalResponse resumeGoal(UUID userId, UUID goalId);
    GoalResponse completeGoal(UUID userId, UUID goalId);
    GoalResponse failGoal(UUID userId, UUID goalId, String reason);
    GoalResponse archiveGoal(UUID userId, UUID goalId);
    
    // Progress
    GoalResponse updateProgress(UUID userId, UUID goalId, int progress);
    GoalResponse recalculateProgress(UUID userId, UUID goalId);
    
    // Milestones
    MilestoneResponse createMilestone(UUID userId, UUID goalId, CreateMilestoneRequest request);
    MilestoneResponse updateMilestone(UUID userId, UUID goalId, UUID milestoneId, UpdateMilestoneRequest request);
    MilestoneResponse completeMilestone(UUID userId, UUID goalId, UUID milestoneId);
    void deleteMilestone(UUID userId, UUID goalId, UUID milestoneId);
    List<MilestoneResponse> getMilestones(UUID userId, UUID goalId);
    
    // Statistics
    GoalStatisticsResponse getStatistics(UUID userId);
    List<GoalResponse> getGoalsByCategory(UUID userId, String category);
    List<GoalResponse> getGoalsByStatus(UUID userId, GoalStatus status);
}
```

### 15.2 REST API Design

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/goals` | Create goal | User |
| GET | `/goals` | List goals (paginated) | User |
| GET | `/goals/{id}` | Get goal details | User |
| PUT | `/goals/{id}` | Update goal | User |
| DELETE | `/goals/{id}` | Delete goal | User |
| POST | `/goals/{id}/start` | Start goal | User |
| POST | `/goals/{id}/pause` | Pause goal | User |
| POST | `/goals/{id}/resume` | Resume goal | User |
| POST | `/goals/{id}/complete` | Complete goal | User |
| POST | `/goals/{id}/fail` | Fail goal | User |
| POST | `/goals/{id}/archive` | Archive goal | User |
| PUT | `/goals/{id}/progress` | Update progress | User |
| GET | `/goals/{id}/milestones` | List milestones | User |
| POST | `/goals/{id}/milestones` | Create milestone | User |
| PUT | `/goals/{id}/milestones/{mid}` | Update milestone | User |
| POST | `/goals/{id}/milestones/{mid}/complete` | Complete milestone | User |
| DELETE | `/goals/{id}/milestones/{mid}` | Delete milestone | User |
| GET | `/goals/statistics` | Get goal statistics | User |
| GET | `/goals/category/{category}` | Get goals by category | User |
| GET | `/goals/status/{status}` | Get goals by status | User |

### 15.3 DTOs

```java
// Request DTOs
public record CreateGoalRequest(
    String title,
    String description,
    String category,
    Priority priority,
    Difficulty difficulty,
    GoalType type,
    Visibility visibility,
    Integer estimatedXp,
    Integer targetDate, // epoch days
    String completionStrategy,
    List<String> tags
) {}

public record UpdateGoalRequest(
    String title,
    String description,
    String category,
    Priority priority,
    Difficulty difficulty,
    Visibility visibility,
    Integer estimatedXp,
    Integer targetDate,
    String completionStrategy,
    List<String> tags
) {}

public record CreateMilestoneRequest(
    String title,
    String description,
    Integer displayOrder
) {}

public record UpdateMilestoneRequest(
    String title,
    String description,
    Integer displayOrder
) {}

// Response DTOs
public record GoalResponse(
    UUID id,
    UUID userId,
    String title,
    String description,
    String category,
    Priority priority,
    Difficulty difficulty,
    GoalStatus status,
    Visibility visibility,
    Integer estimatedXp,
    Integer currentProgress,
    Double completionPercentage,
    Integer targetDate,
    Integer completedDate,
    Integer archivedDate,
    String completionStrategy,
    List<String> tags,
    Map<String, Object> customMetadata,
    Instant createdAt,
    Instant updatedAt,
    List<MilestoneResponse> milestones
) {}

public record MilestoneResponse(
    UUID id,
    UUID goalId,
    String title,
    String description,
    Integer displayOrder,
    Boolean isCompleted,
    Integer completedDate,
    Instant createdAt,
    Instant updatedAt
) {}

public record GoalStatisticsResponse(
    long totalGoals,
    long activeGoals,
    long completedGoals,
    long failedGoals,
    long archivedGoals,
    double averageCompletionPercentage,
    long totalMilestones,
    long completedMilestones,
    Map<GoalStatus, Long> goalsByStatus,
    Map<Priority, Long> goalsByPriority,
    Map<Difficulty, Long> goalsByDifficulty
) {}
```

---

## 16. Validation Strategy

### 16.1 Goal Validation

| Field | Rule |
|-------|------|
| `title` | Required, 1-255 characters |
| `description` | Optional, max 5000 characters |
| `category` | Optional, max 50 characters |
| `priority` | Required, must be valid enum value |
| `difficulty` | Optional, must be valid enum value |
| `status` | Required, must be valid enum value |
| `visibility` | Required, must be valid enum value |
| `estimated_xp` | Optional, must be >= 0 |
| `completion_percentage` | Must be 0-100 |
| `target_date` | Optional, must be in the future when setting |
| `tags` | Optional, array of strings, max 10 tags |
| `custom_metadata` | Optional, valid JSON |

### 16.2 Milestone Validation

| Field | Rule |
|-------|------|
| `title` | Required, 1-255 characters |
| `description` | Optional, max 2000 characters |
| `display_order` | Required, must be >= 0 |
| `is_completed` | Boolean |

### 16.3 State Transition Validation

- Cannot complete a DRAFT goal without starting it first
- Cannot archive a goal without it being in a terminal state
- Cannot resume an ARCHIVED goal (must create new goal)
- Completing a goal requires `completion_percentage >= 100` OR manual override

---

## 17. Lifecycle

### 17.1 Goal Lifecycle States

```
DRAFT ──▶ ACTIVE ──▶ PAUSED ──▶ ACTIVE
  │              │              │
  │              │              └──▶ ARCHIVED
  │              │
  │              ├──▶ COMPLETED ──▶ ARCHIVED
  │              │
  │              ├──▶ FAILED ──▶ ACTIVE (retry)
  │              │
  │              └──▶ ARCHIVED
  │
  └──▶ ARCHIVED (discard)
```

### 17.2 State Timestamps

| State | Timestamp Set |
|-------|--------------|
| DRAFT → ACTIVE | `started_date` (future field) |
| ACTIVE → COMPLETED | `completed_date` |
| ACTIVE → FAILED | `failed_date` (future field) |
| Any → ARCHIVED | `archived_date` |

### 17.3 Automatic Transitions

- Goals with `target_date` in the past and status ACTIVE are considered OVERDUE
- Overdue goals can trigger notifications (Notification Engine)
- Failed goals can be retried (FAILED → ACTIVE) with progress reset

---

## 18. Future Expansion

### 18.1 Planned Enhancements

1. **Goal Dependencies**: Goal B cannot start until Goal A is complete
2. **Goal Sharing**: Share goals with friends or team members
3. **Goal Templates**: Pre-defined goal templates for common objectives
4. **Goal Suggestions**: AI-suggested goals based on user patterns
5. **Goal Challenges**: Time-limited goal competitions
6. **Goal Progress Photos/Notes**: Rich media attachments for milestones
7. **Goal Reflection**: Post-completion review and lessons learned
8. **Goal Grouping**: Parent/child goal hierarchies
9. **Goal Reminders**: Smart reminders based on target date and progress
10. **Goal Analytics**: Detailed analytics on goal achievement patterns

### 18.2 Extensibility Points

- `custom_metadata` JSONB for engine-specific extensions
- `completion_strategy` for pluggable completion logic
- `tags` JSONB for flexible categorization
- Domain events for reactive integrations
- Service interface for programmatic access

---

## 19. Scalability

### 19.1 Small Scale (100 goals per user)

- Single database query per goal list with pagination
- In-memory caching of active goals
- No special optimization needed

### 19.2 Medium Scale (1000 goals per user)

- Pagination required for goal lists (default 20 per page)
- Index on `(user_id, status, target_date)` for efficient filtering
- Cache hit ratio > 90% for active goals
- Milestone queries optimized by `display_order` index

### 19.3 Large Scale (10000+ goals per user)

- **Database**: Composite indexes on `(user_id, status)`, `(user_id, category)`, `(user_id, target_date)`
- **Pagination**: Cursor-based pagination for large result sets
- **Archiving**: Automatic archiving of old goals to maintain query performance
- **Caching**: L2 cache per user for active goals
- **Architecture**: Consider read replicas for analytics queries
- **No Redesign Needed**: Current schema supports unlimited growth

### 19.4 Growth Projections

| Users | Goals/User | Total Rows | Query Time (indexed) |
|-------|------------|------------|---------------------|
| 10K | 100 | 1M | < 5ms |
| 100K | 100 | 10M | < 10ms |
| 1M | 100 | 100M | < 20ms |
| 10M | 1000 | 10B | Requires archiving strategy |

---

## 20. Authorization Model

### 20.1 Access Rules

| Operation | Owner | Admin | Others |
|-----------|-------|-------|--------|
| Create goal | Allowed | Allowed | Denied |
| Read own goals | Allowed | Allowed | Denied |
| Update own goals | Allowed | Allowed | Denied |
| Delete own goals | Allowed | Allowed | Denied |
| Read other user's public goals | Depends on visibility | Allowed (audit) | Denied |
| Update other user's goals | Never | Allowed (admin only) | Never |
| Delete other user's goals | Never | Allowed (admin only) | Never |

### 20.2 Visibility Levels

| Visibility | Description |
|------------|-------------|
| `PRIVATE` | Only owner can view |
| `FRIENDS` | Owner and friends can view |
| `PUBLIC` | Anyone can view |

---

## 21. Risks and Trade-offs

### 21.1 Risks

| Risk | Mitigation |
|------|-----------|
| Progress calculation complexity | Keep strategies simple, defer complex logic to v2 |
| Milestone/task coupling | Clear service boundaries, event-driven updates |
| State transition errors | Explicit state machine with validation |
| Performance with many milestones | Index on `(goal_id, display_order)` |
| Circular dependencies with Task Engine | Strict Rule 34 enforcement, service-only access |
| Goal explosion (too many goals) | Archiving strategy, dashboard limits |

### 21.2 Trade-offs

| Decision | Trade-off |
|----------|-----------|
| Hybrid progress vs pure automatic | Hybrid: flexible, but more complex |
| Milestones as separate table vs embedded JSON | Separate table: queryable, but more joins |
| Goal types as enum vs separate table | Enum: simple, but less flexible |
| Manual archiving vs automatic | Manual: user control, but requires discipline |
| JSONB tags vs separate table | JSONB: flexible, but harder to query |

---

## 22. Implementation Phases

### Phase 1 (v0.6.0) - Foundation

- V7 migration: goals and goal_milestones tables
- Goal entity with all fields
- Goal repository with standard queries
- Goal DTOs and mapper
- GoalService interface and implementation
- GoalController with CRUD endpoints
- Basic status lifecycle (DRAFT, ACTIVE, COMPLETED, ARCHIVED)
- Priority and difficulty enums
- Manual progress tracking
- Milestone CRUD
- GoalExceptionHandler
- Unit and integration tests
- docs/goal/README.md

### Phase 2 (v0.7.0) - Integration

- Domain events (GoalCreated, GoalCompleted, etc.)
- Task Engine integration for automatic progress
- XP Engine integration for rewards
- Notification Engine integration for reminders
- Analytics Engine integration for reporting
- Advanced completion strategies
- Goal statistics

### Phase 3 (v1.0.0) - Production

- Goal dependencies
- Goal templates
- AI suggestions
- Goal sharing
- Advanced analytics
- Performance optimization
- Full test coverage

---

## 23. Open Questions

1. Should goals support parent/child hierarchies (sub-goals)?
2. Should goal templates be user-created or system-provided?
3. Should failed goals count toward user statistics negatively?
4. What is the maximum number of milestones per goal?
5. Should goal completion trigger automatic archiving?
6. Should we support goal collaboration (multiple users, shared goals)?

---

## 24. Approval

- [ ] Lead Architect
- [ ] Product Owner
- [ ] XP Engine Owner
- [ ] Task Engine Owner
- [ ] Analytics Engine Owner

---

*This document is part of THE SYSTEM Backend Architecture. All rules from the Backend Constitution apply.*
