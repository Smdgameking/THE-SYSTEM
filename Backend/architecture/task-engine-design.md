# Task Engine Design Document

## Version: 1.0.0
## Date: 2026-08-07
## Status: Draft - Pending Approval

---

## 1. Purpose

The Task Engine is the execution layer of THE SYSTEM. While Goals define WHAT the user wants to achieve, Tasks define HOW the user reaches those goals. The Task Engine owns every executable action inside THE SYSTEM.

The Task Engine provides:
- Task lifecycle management (creation, execution, completion, archiving)
- Task scheduling and prioritization
- Task dependency management with cycle prevention
- Subtask hierarchy and completion rules
- Recurring task generation
- Time tracking (estimated, actual, focus sessions)
- Task completion evidence and verification
- Statistics and reporting on task execution
- Extension points for AI, Memory, and other engines

---

## 2. Responsibilities

### Owned by Task Engine

- **Task Entities**: Core task data model with lifecycle state
- **Task Lifecycle**: State machine for task status transitions
- **Task Priority**: LOW, NORMAL, HIGH, CRITICAL for prioritization
- **Task Scheduling**: Due dates, reminders, start dates
- **Task Dependencies**: Prerequisite relationships with cycle prevention
- **Task Subtasks**: Hierarchical task decomposition
- **Task Recurrence**: Repeating task generation
- **Task Completion**: Manual and future automatic completion
- **Task Execution History**: Audit trail of task changes
- **Task Statistics**: Completion rates, time tracking, streaks
- **Task Time Tracking**: Estimated duration, actual duration, focus sessions
- **Task Attachments**: Architecture for file and link attachments
- **Task Evidence**: Completion evidence storage
- **Task Execution Types**: BOOLEAN, CHECKLIST, TIMER, COUNT, PROGRESS, HABIT, APPROVAL, CUSTOM
- **Execution Providers**: Pluggable execution behavior per type
- **Execution State**: Structured runtime state for each execution type

### NOT Owned by Task Engine

- Goals (owned by Goal Engine)
- Goal Progress (owned by Goal Engine)
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

The Task Engine exclusively owns:
- `tasks` table
- `task_dependencies` table
- `task_time_entries` table
- `recurring_task_configs` table
- All business logic related to tasks
- Task state transitions
- Dependency resolution and cycle prevention
- Recurring task generation
- Time tracking logic
- Execution types and providers
- Execution state management

### 3.2 Cross-Engine Boundaries

Other engines interact with tasks ONLY through:
- **Task Service Interface**: Published service methods
- **Domain Events**: TaskCreated, TaskCompleted, etc.
- **Goal Engine**: Tasks may belong to goals; Task Engine publishes events, Goal Engine updates progress
- **XP Engine**: Task completion events trigger XP awards
- **Memory Engine**: Task completion may generate memory signals
- **Analytics Engine**: Consumes task data for reporting
- **Notification Engine**: Sends reminders and notifications
- **AI Engine**: Suggests tasks, estimates duration, detects overload
- **Settings Engine**: User preferences for task defaults

No engine may directly query or modify the `tasks`, `task_dependencies`, `task_time_entries`, or `recurring_task_configs` tables.

---

## 4. Database Design

### 4.1 Tasks Table

```sql
CREATE TABLE IF NOT EXISTS tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    goal_id UUID NULL,
    parent_task_id UUID NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    category VARCHAR(50),
    execution_type VARCHAR(20) NOT NULL DEFAULT 'BOOLEAN',
    estimated_duration INTEGER NULL,
    actual_duration INTEGER NULL,
    start_date TIMESTAMP NULL,
    due_date TIMESTAMP NULL,
    completed_date TIMESTAMP NULL,
    reminder_date TIMESTAMP NULL,
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    recurring_config_id UUID NULL,
    tags JSONB NULL,
    attachments JSONB NULL,
    notes TEXT,
    completion_evidence JSONB NULL,
    execution_state JSONB NULL,
    custom_metadata JSONB NULL,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_goal FOREIGN KEY (goal_id) REFERENCES goals(id) ON DELETE SET NULL,
    CONSTRAINT fk_tasks_parent FOREIGN KEY (parent_task_id) REFERENCES tasks(id) ON DELETE CASCADE
);
```

### 4.2 Task Dependencies Table

```sql
CREATE TABLE IF NOT EXISTS task_dependencies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL,
    depends_on_task_id UUID NOT NULL,
    dependency_type VARCHAR(20) NOT NULL DEFAULT 'BLOCKS',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    resolved_date TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_task_dependencies_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_dependencies_depends_on FOREIGN KEY (depends_on_task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT uq_task_dependency UNIQUE (task_id, depends_on_task_id)
);
```

### 4.3 Task Time Entries Table

```sql
CREATE TABLE IF NOT EXISTS task_time_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL,
    user_id UUID NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NULL,
    duration_minutes INTEGER NULL,
    entry_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_task_time_entries_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_time_entries_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

### 4.4 Recurring Task Configs Table

```sql
CREATE TABLE IF NOT EXISTS recurring_task_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    interval_value INTEGER NOT NULL DEFAULT 1,
    cron_expression VARCHAR(100) NULL,
    days_of_week INTEGER[] NULL,
    day_of_month INTEGER NULL,
    month INTEGER NULL,
    exception_dates DATE[] NULL,
    end_date TIMESTAMP NULL,
    max_occurrences INTEGER NULL,
    occurrence_count INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    CONSTRAINT fk_recurring_task_configs_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);
```

### 4.5 Indexes

```sql
-- User tasks
CREATE INDEX IF NOT EXISTS idx_tasks_user_id ON tasks(user_id);
CREATE INDEX IF NOT EXISTS idx_tasks_user_status ON tasks(user_id, status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_user_due_date ON tasks(user_id, due_date) WHERE deleted_at IS NULL AND due_date IS NOT NULL;

-- Filtering
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_priority ON tasks(priority) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_category ON tasks(category) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_due_date ON tasks(due_date) WHERE deleted_at IS NULL AND due_date IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_goal_id ON tasks(goal_id) WHERE deleted_at IS NULL AND goal_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_parent_task_id ON tasks(parent_task_id) WHERE deleted_at IS NULL AND parent_task_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_is_recurring ON tasks(is_recurring) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_execution_type ON tasks(execution_type) WHERE deleted_at IS NULL;

-- Dependencies
CREATE INDEX IF NOT EXISTS idx_task_dependencies_task_id ON task_dependencies(task_id);
CREATE INDEX IF NOT EXISTS idx_task_dependencies_depends_on ON task_dependencies(depends_on_task_id);

-- Time entries
CREATE INDEX IF NOT EXISTS idx_task_time_entries_task_id ON task_time_entries(task_id);
CREATE INDEX IF NOT EXISTS idx_task_time_entries_user_id ON task_time_entries(user_id);

-- Recurring configs
CREATE INDEX IF NOT EXISTS idx_recurring_task_configs_task_id ON recurring_task_configs(task_id);
CREATE INDEX IF NOT EXISTS idx_recurring_task_configs_is_active ON recurring_task_configs(is_active) WHERE is_active = TRUE;

-- Soft delete
CREATE INDEX IF NOT EXISTS idx_tasks_deleted_at ON tasks(deleted_at);
CREATE INDEX IF NOT EXISTS idx_task_dependencies_deleted_at ON task_dependencies(deleted_at);
CREATE INDEX IF NOT EXISTS idx_task_time_entries_deleted_at ON task_time_entries(deleted_at);
```

### 4.6 Entity Relationships

```
users (1) ─── (N) tasks
    │
    └── user_id FK with ON DELETE CASCADE

goals (1) ─── (N) tasks
    │
    └── goal_id FK with ON DELETE SET NULL

tasks (1) ─── (N) tasks (subtasks)
    │
    └── parent_task_id FK with ON DELETE CASCADE

tasks (1) ─── (N) task_dependencies
    │
    └── task_id FK with ON DELETE CASCADE

tasks (1) ─── (N) task_time_entries
    │
    └── task_id FK with ON DELETE CASCADE

tasks (1) ─── (1) recurring_task_configs
    │
    └── task_id FK with ON DELETE CASCADE
```

---

## 5. Task Model

### 5.1 Field Justification

| Field | Type | Justification |
|-------|------|---------------|
| `id` | UUID | Primary key, distributed-system friendly |
| `user_id` | UUID | FK to users, every task belongs to a user |
| `goal_id` | UUID | FK to goals (nullable), optional goal association |
| `parent_task_id` | UUID | FK to tasks (nullable), for subtask hierarchy |
| `title` | VARCHAR(255) | Required, concise task name |
| `description` | TEXT | Optional, detailed task description |
| `status` | VARCHAR(20) | DRAFT, PENDING, IN_PROGRESS, BLOCKED, PAUSED, COMPLETED, FAILED, CANCELLED, ARCHIVED |
| `priority` | VARCHAR(20) | LOW, NORMAL, HIGH, CRITICAL for prioritization |
| `execution_type` | VARCHAR(20) | BOOLEAN, CHECKLIST, TIMER, COUNT, PROGRESS, HABIT, APPROVAL, CUSTOM |
| `category` | VARCHAR(50) | User-defined category for organization |
| `estimated_duration` | INTEGER | Estimated time in minutes |
| `actual_duration` | INTEGER | Accumulated actual time in minutes |
| `start_date` | TIMESTAMP | When task is scheduled to start |
| `due_date` | TIMESTAMP | Deadline for task completion |
| `completed_date` | TIMESTAMP | Set when status becomes COMPLETED |
| `reminder_date` | TIMESTAMP | When to remind user about task |
| `is_recurring` | BOOLEAN | Flag indicating task has recurrence configuration |
| `recurring_config_id` | UUID | FK to recurring_task_configs (nullable) |
| `tags` | JSONB | Flexible tagging for filtering |
| `attachments` | JSONB | Architecture-only: stores references (URLs, IDs), not files |
| `notes` | TEXT | User notes about task execution |
| `completion_evidence` | JSONB | Evidence data for completion (screenshots, checklists, etc.) |
| `execution_state` | JSONB | Structured runtime state for the execution type |
| `custom_metadata` | JSONB | Engine-specific extensions without schema changes |
| `visibility` | VARCHAR(20) | PRIVATE, FRIENDS, PUBLIC for sharing |
| `created_at` | TIMESTAMP | Audit field from BaseEntity |
| `updated_at` | TIMESTAMP | Audit field from BaseEntity |
| `created_by` | UUID | Audit field from BaseEntity |
| `updated_by` | UUID | Audit field from BaseEntity |
| `deleted_at` | TIMESTAMP | Soft delete from BaseEntity |

### 5.2 Field Decisions

- **`execution_type`**: Determines which Execution Provider manages the task. Default is BOOLEAN.
- **`execution_state`**: JSONB column storing structured state for the execution type. Replaces generic "completion payload" concept.
- **`actual_duration`**: Accumulated from `task_time_entries`. Updated automatically when time entries are created.
- **`attachments`**: JSONB array storing references (file IDs, URLs, etc.). Actual file storage is delegated to a storage service. Task Engine only stores metadata.
- **`completion_evidence`**: JSONB storing structured evidence (checklist results, verification data, notes). Enables future AI validation.
- **`recurring_config_id`**: Links to separate config table for clean separation of concerns.
- **`parent_task_id`**: Self-referencing FK for subtask hierarchy. NULL indicates top-level task.
- **`goal_id`**: Optional link to goal. ON DELETE SET NULL ensures task survival if goal is deleted.

---

## 6. Task Lifecycle State Machine

### 6.1 Task Statuses

The Task Engine uses a strict state machine to govern task lifecycle. Transitions are validated to prevent invalid state changes.

| Status | Description | Allowed Transitions |
|--------|-------------|---------------------|
| `DRAFT` | Task is being created, not yet ready for execution | PENDING, CANCELLED |
| `PENDING` | Task is queued for execution, dependencies may be unresolved | IN_PROGRESS, CANCELLED, ARCHIVED |
| `IN_PROGRESS` | Task is actively being worked on | PAUSED, BLOCKED, COMPLETED, FAILED |
| `BLOCKED` | Task cannot proceed due to unresolved dependencies | IN_PROGRESS, CANCELLED |
| `PAUSED` | Task execution is temporarily halted | IN_PROGRESS, CANCELLED |
| `COMPLETED` | Task has been successfully finished | ARCHIVED |
| `FAILED` | Task execution failed | PENDING, CANCELLED |
| `CANCELLED` | Task was intentionally abandoned | ARCHIVED |
| `ARCHIVED` | Task is finalized and stored for history | (terminal state) |

### 6.2 State Transition Rules

```yaml
transitions:
  DRAFT:
    - PENDING
    - CANCELLED
  PENDING:
    - IN_PROGRESS
    - CANCELLED
    - ARCHIVED
  IN_PROGRESS:
    - PAUSED
    - BLOCKED
    - COMPLETED
    - FAILED
  BLOCKED:
    - IN_PROGRESS
    - CANCELLED
  PAUSED:
    - IN_PROGRESS
    - CANCELLED
  COMPLETED:
    - ARCHIVED
  FAILED:
    - PENDING
    - CANCELLED
  CANCELLED:
    - ARCHIVED
  ARCHIVED: []
```

### 6.3 Transition Side Effects

| Transition | Side Effects |
|------------|-------------|
| DRAFT → PENDING | Validate required fields, set `start_date` if not set, publish `TaskActivated` event |
| PENDING → IN_PROGRESS | Set `actual_duration` start tracking, publish `TaskStarted` event |
| IN_PROGRESS → BLOCKED | Identify blocking dependencies, publish `TaskBlocked` event |
| BLOCKED → IN_PROGRESS | Verify dependencies resolved, publish `TaskUnblocked` event |
| IN_PROGRESS → COMPLETED | Set `completed_date`, calculate `actual_duration`, aggregate time entries, publish `TaskCompleted` event |
| COMPLETED → ARCHIVED | Mark as read-only, publish `TaskArchived` event |
| IN_PROGRESS → FAILED | Record failure reason, publish `TaskFailed` event |
| FAILED → PENDING | Reset execution state, allow retry |
| Any → CANCELLED | Record cancellation reason, release resources, publish `TaskCancelled` event |

### 6.4 Lifecycle Hooks

The Task Engine supports hooks at each lifecycle stage:

```yaml
hooks:
  before_transition:
    - validate_transition
    - check_permissions
    - validate_dependencies
  after_transition:
    - update_statistics
    - publish_domain_event
    - trigger_notifications
    - update_goal_progress
    - award_xp
```

---

## 7. Task Priority System

### 7.1 Priority Levels

Tasks are prioritized using a four-level system:

| Priority | Value | Use Case | Scheduling Weight |
|----------|-------|----------|-------------------|
| `LOW` | 1 | Nice-to-have tasks with no deadline pressure | 1 |
| `NORMAL` | 2 | Standard tasks with expected timelines | 2 |
| `HIGH` | 3 | Important tasks requiring timely completion | 3 |
| `CRITICAL` | 4 | Urgent tasks with immediate impact | 4 |

### 7.2 Priority Rules

- Default priority is `NORMAL`
- Priority can be set by user or AI Engine suggestion
- `CRITICAL` tasks bypass dependency blocking (configurable)
- Priority influences task ordering in the task queue
- Priority is considered alongside due date for scheduling decisions

### 7.3 Priority Change Audit

All priority changes are recorded in the execution history with:
- Previous priority
- New priority
- Changed by (user_id or system)
- Reason (user-provided or system-detected)

---

## 8. Task Scheduling

### 8.1 Scheduling Fields

| Field | Type | Purpose |
|-------|------|---------|
| `start_date` | TIMESTAMP | Earliest time the task can begin |
| `due_date` | TIMESTAMP | Hard deadline for task completion |
| `reminder_date` | TIMESTAMP | When to notify the user about the task |

### 8.2 Scheduling Rules

- `start_date` must be <= `due_date` if both are set
- `reminder_date` must be <= `due_date` if both are set
- `reminder_date` can be before `start_date` for advance notice
- Tasks with `start_date` in the future are hidden from active task lists until the start date arrives
- Overdue tasks (due_date < now() and status not in COMPLETED, CANCELLED, ARCHIVED) are flagged in UI

### 8.3 Reminder System

The Task Engine publishes `TaskReminderDue` events at `reminder_date`. The Notification Engine is responsible for delivering these reminders via the user's preferred channels.

Reminder behavior:
- Single reminder at `reminder_date`
- Recurring reminders are NOT handled by Task Engine; use recurring tasks with multiple instances
- If task is completed before reminder, reminder is cancelled

---

## 9. Task Dependencies

### 9.1 Dependency Types

| Type | Code | Description |
|------|------|-------------|
| `BLOCKS` | BLOCKS | Task B cannot start until Task A is complete |
| `RELATED` | RELATED | Tasks are related but do not block each other |

### 9.2 Cycle Prevention

The Task Engine MUST prevent circular dependencies using a directed acyclic graph (DAG) validation algorithm.

**Algorithm: Topological Sort with Cycle Detection**

```yaml
algorithm:
  1. Build adjacency list from task_dependencies
  2. Perform Kahn's algorithm for topological sort
  3. If cycle detected:
     a. Reject the dependency creation
     b. Return error: "Circular dependency detected"
     c. Publish TaskDependencyCycleDetected event
  4. If no cycle:
     a. Create the dependency
     b. Update BLOCKED status for affected tasks
     c. Publish TaskDependencyCreated event
```

**Complexity**: O(V + E) where V = tasks, E = dependencies

### 9.3 Dependency Resolution

When a task's dependency is completed:
1. Check if all dependencies for the blocked task are resolved
2. If all resolved, transition task from BLOCKED to PENDING
3. Publish `TaskDependencyResolved` event

When a task's dependency is cancelled or fails:
1. Remove the dependency or mark as unresolved
2. Re-evaluate the blocked task's status
3. Publish `TaskDependencyChanged` event

### 9.4 Dependency Constraints

- A task cannot depend on itself
- A task cannot have duplicate dependencies
- Dependencies can only be created between tasks owned by the same user
- Dependencies on ARCHIVED tasks are invalid
- Dependencies on CANCELLED tasks are automatically removed

---

## 10. Subtasks

### 10.1 Subtask Hierarchy

Tasks support unlimited nesting through `parent_task_id`:

```
Task (parent_task_id = NULL)
├── Subtask (parent_task_id = Task.id)
│   ├── Sub-subtask (parent_task_id = Subtask.id)
│   └── Sub-subtask (parent_task_id = Subtask.id)
└── Subtask (parent_task_id = Task.id)
```

### 10.2 Subtask Completion Rules

| Rule | Description |
|------|-------------|
| Independent | Subtasks can be completed in any order |
| Parent blocking | Parent task cannot be completed until all subtasks are completed |
| Parent auto-complete | Optionally, completing all subtasks auto-completes the parent |
| Progress aggregation | Parent progress = (completed_subtasks / total_subtasks) * 100 |
| Independent completion | Subtasks can be completed independently of parent status |

### 10.3 Subtask Operations

- **Create subtask**: Sets `parent_task_id`, inherits `user_id` and `goal_id` from parent
- **Delete subtask**: Cascades to sub-subtasks (ON DELETE CASCADE)
- **Move subtask**: Change `parent_task_id` to re-parent under another task
- **Complete subtask**: Updates parent progress, triggers parent completion check if configured

---

## 11. Recurring Tasks

### 11.1 Recurrence Configuration

Recurring tasks use a separate `recurring_task_configs` table to store recurrence rules:

| Field | Type | Purpose |
|-------|------|---------|
| `frequency` | VARCHAR(20) | DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM |
| `interval_value` | INTEGER | Repeat every N units (e.g., every 2 weeks) |
| `cron_expression` | VARCHAR(100) | For CUSTOM frequency, standard cron syntax |
| `days_of_week` | INTEGER[] | For WEEKLY: which days (0=Sunday, 6=Saturday) |
| `day_of_month` | INTEGER | For MONTHLY: which day of month |
| `month` | INTEGER | For YEARLY: which month (1-12) |
| `exception_dates` | DATE[] | Dates to skip (holidays, vacations) |
| `end_date` | TIMESTAMP | When recurrence stops generating tasks |
| `max_occurrences` | INTEGER | Maximum number of generated tasks |
| `occurrence_count` | INTEGER | Counter of generated tasks |
| `is_active` | BOOLEAN | Whether recurrence is currently active |

### 11.2 Recurrence Generation

The Task Engine generates new task instances via a scheduled background job:

```yaml
generation_rules:
  trigger: When a recurring task is completed
  timing: Generate next occurrence immediately after completion
  conditions:
    - is_active = TRUE
    - (end_date IS NULL OR end_date > NOW())
    - (max_occurrences IS NULL OR occurrence_count < max_occurrences)
  behavior:
    - Create new task with same title, description, priority, etc.
    - Reset status to PENDING
    - Clear completion fields (completed_date, actual_duration, etc.)
    - Increment occurrence_count
    - Calculate next due_date based on frequency
    - Exclude exception_dates from next occurrence calculation
```

### 11.3 Recurrence Termination

Recurrence stops when ANY of the following conditions are met:
- `is_active` is set to FALSE
- `end_date` has passed
- `occurrence_count` >= `max_occurrences`
- The parent task is deleted
- The parent task is archived

### 11.4 Exception Handling

- If a recurring task instance is skipped (not completed), the next instance is still generated
- If a recurring task is cancelled, only that instance is cancelled; recurrence continues
- If a recurring task is failed, recurrence continues (user may need to manually address)

---

## 12. Task Completion

### 12.1 Completion Criteria

A task can be marked COMPLETED when:

| Condition | Requirement |
|-----------|-------------|
| No dependencies | All BLOCKS dependencies are COMPLETED |
| Subtasks | All subtasks are COMPLETED (if parent completion rule is enabled) |
| Execution type | Execution Provider validates completion criteria |
| User action | User explicitly marks task as complete |

### 12.2 Completion Process

```yaml
completion_flow:
  1. Validate all BLOCKS dependencies are COMPLETED
  2. Validate all subtasks are COMPLETED (if rule enabled)
  3. Execute Execution Provider complete() method
  4. Collect completion evidence from execution state
  5. Set status = COMPLETED
  6. Set completed_date = NOW()
  7. Calculate actual_duration from time entries
  8. Publish TaskCompleted event
  9. Trigger downstream events (XP award, goal progress, memory signal)
  10. If recurring, generate next occurrence
```

### 12.3 Completion Evidence

The `completion_evidence` JSONB field stores structured evidence:

```json
{
  "type": "checklist|timer|count|progress|approval|custom",
  "completed_at": "2026-08-07T10:00:00Z",
  "data": {
    "checklist_items_completed": 5,
    "checklist_items_total": 5,
    "timer_duration_minutes": 25,
    "approval_notes": "Approved by manager"
  },
  "validated_by": "user_id_or_system",
  "validation_notes": "Optional validation notes"
}
```

### 12.4 Automatic Completion

Future extension points for automatic completion:
- AI Engine may mark tasks complete based on evidence analysis
- Integration with external systems may trigger completion
- Time-based auto-completion for habits (e.g., if not completed by end of day, mark as missed)

---

## 13. Execution Types and Providers

### 13.1 Execution Types

The Task Engine supports multiple execution types, each with a dedicated Execution Provider:

| Type | Code | Description | Default Provider |
|------|------|-------------|-----------------|
| Boolean | BOOLEAN | Simple yes/no completion | BooleanExecutionProvider |
| Checklist | CHECKLIST | List of items to complete | ChecklistExecutionProvider |
| Timer | TIMER | Time-based task with focus sessions | TimerExecutionProvider |
| Count | COUNT | Repeat N times (e.g., 10 pushups) | CountExecutionProvider |
| Progress | PROGRESS | Percentage-based progress (0-100%) | ProgressExecutionProvider |
| Habit | HABIT | Recurring behavior tracking | HabitExecutionProvider |
| Approval | APPROVAL | Requires approval from another user | ApprovalExecutionProvider |
| Custom | CUSTOM | Extensible for AI or custom logic | CustomExecutionProvider |

### 13.2 Execution Provider Interface

```yaml
interface: ExecutionProvider
methods:
  initialize(task: Task): ExecutionState
    - Set up initial execution state
    - Return structured state for the execution type
  
  validate_completion(task: Task, state: ExecutionState): ValidationResult
    - Check if task meets completion criteria
    - Return valid/invalid with reasons
  
  complete(task: Task, state: ExecutionState, evidence: Json): ExecutionState
    - Process completion
    - Update execution state
    - Return final state
  
  reset(task: Task, state: ExecutionState): ExecutionState
    - Reset task for re-execution
    - Clear execution state
  
  get_progress(task: Task, state: ExecutionState): number
    - Return completion percentage (0-100)
  
  can_retry(task: Task, state: ExecutionState): boolean
    - Determine if task can be retried after failure
```

### 13.3 Execution State Schema

The `execution_state` JSONB column stores type-specific state:

**BOOLEAN**:
```json
{ "completed": false }
```

**CHECKLIST**:
```json
{
  "items": [
    { "id": "uuid", "text": "Item 1", "completed": true },
    { "id": "uuid", "text": "Item 2", "completed": false }
  ],
  "total": 5,
  "completed_count": 1
}
```

**TIMER**:
```json
{
  "focus_sessions": [
    { "started_at": "2026-08-07T09:00:00Z", "ended_at": "2026-08-07T09:25:00Z", "duration_minutes": 25 }
  ],
  "total_focus_minutes": 50,
  "target_minutes": 120,
  "is_running": false,
  "current_session_started_at": null
}
```

**COUNT**:
```json
{
  "target_count": 10,
  "current_count": 3,
  "increments": [
    { "timestamp": "2026-08-07T09:00:00Z", "count": 1 },
    { "timestamp": "2026-08-07T09:05:00Z", "count": 2 }
  ]
}
```

**PROGRESS**:
```json
{
  "percentage": 45,
  "milestones": [
    { "percentage": 25, "achieved_at": "2026-08-07T08:00:00Z" },
    { "percentage": 50, "achieved_at": null }
  ]
}
```

**HABIT**:
```json
{
  "streak": 5,
  "longest_streak": 12,
  "last_completed_date": "2026-08-07",
  "schedule": {
    "frequency": "DAILY",
    "time_of_day": "09:00:00"
  },
  "missed_count": 2
}
```

**APPROVAL**:
```json
{
  "required_approver_id": "uuid",
  "approval_status": "PENDING",
  "approval_notes": "",
  "approved_by": null,
  "approved_at": null
}
```

**CUSTOM**:
```json
{
  "provider": "ai_generated",
  "config": {},
  "state": {}
}
```

### 13.4 Provider Registration

Execution Providers are registered via dependency injection:

```yaml
registration:
  - BooleanExecutionProvider
  - ChecklistExecutionProvider
  - TimerExecutionProvider
  - CountExecutionProvider
  - ProgressExecutionProvider
  - HabitExecutionProvider
  - ApprovalExecutionProvider
  - CustomExecutionProvider

extension_point:
  - AI Engine can register CustomExecutionProvider instances
  - Third-party integrations can register via plugin system
```

---

## 14. Time Tracking

### 14.1 Time Entry Types

| Type | Code | Description |
|------|------|-------------|
| Manual | MANUAL | User manually logs time |
| Timer | TIMER | Automatic timer during focus session |
| Pomodoro | POMODORO | Pomodoro technique intervals |
| Break | BREAK | Break time between sessions |

### 14.2 Time Entry Model

The `task_time_entries` table tracks all time spent on tasks:

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID | Primary key |
| `task_id` | UUID | FK to task |
| `user_id` | UUID | FK to user |
| `start_time` | TIMESTAMP | When the time entry started |
| `end_time` | TIMESTAMP | When the time entry ended (null if running) |
| `duration_minutes` | INTEGER | Calculated duration in minutes |
| `entry_type` | VARCHAR(20) | MANUAL, TIMER, POMODORO, BREAK |
| `notes` | TEXT | Optional notes about this time entry |

### 14.3 Time Aggregation

- `task.actual_duration` is the sum of all `duration_minutes` from `task_time_entries` for that task
- Updated automatically via database trigger or application logic when time entries are created/updated/deleted
- Only non-deleted time entries are counted

### 14.4 Focus Sessions

Focus sessions are a specialized time tracking feature:
- User starts a focus session on a task
- Timer runs automatically
- Session ends when user stops or task is completed
- Multiple focus sessions can be active on different tasks (but not the same task concurrently)
- Focus session data is stored in `execution_state` for TIMER type tasks

---

## 15. Task Attachments

### 15.1 Attachment Architecture

The Task Engine does NOT store files directly. Instead, it stores metadata references:

```json
[
  {
    "id": "uuid",
    "type": "file|link|image",
    "name": "document.pdf",
    "url": "https://storage.example.com/files/abc123",
    "file_id": "storage_file_uuid",
    "size_bytes": 1024000,
    "mime_type": "application/pdf",
    "uploaded_at": "2026-08-07T10:00:00Z",
    "uploaded_by": "user_id"
  }
]
```

### 15.2 Attachment Operations

- **Add attachment**: Store reference metadata, upload file to storage service
- **Remove attachment**: Delete reference, optionally delete file from storage
- **List attachments**: Return metadata for all non-deleted attachments
- **Download attachment**: Delegate to storage service using stored URL or file_id

### 15.3 Storage Service Integration

The Task Engine calls a Storage Service interface:

```yaml
StorageService:
  upload(file: Binary, mime_type: String): StorageResult
  download(file_id: UUID): Binary
  delete(file_id: UUID): boolean
  get_url(file_id: UUID): String
```

---

## 16. Task Statistics

### 16.1 Computed Statistics

The Task Engine computes and caches the following statistics:

| Statistic | Description | Calculation |
|-----------|-------------|-------------|
| `completion_rate` | Percentage of completed tasks | completed_count / total_count * 100 |
| `total_tasks` | Total number of tasks | COUNT(tasks) |
| `completed_tasks` | Number of completed tasks | COUNT(tasks) WHERE status = COMPLETED |
| `overdue_tasks` | Number of overdue tasks | COUNT(tasks) WHERE due_date < NOW() AND status NOT IN (COMPLETED, CANCELLED, ARCHIVED) |
| `average_completion_time` | Average days to complete tasks | AVG(completed_date - created_at) |
| `streak_days` | Consecutive days with completed tasks | Consecutive days with at least one COMPLETED task |
| `focus_time_today` | Total focus minutes today | SUM(duration_minutes) FROM task_time_entries WHERE date = TODAY() |
| `focus_time_week` | Total focus minutes this week | SUM(duration_minutes) FROM task_time_entries WHERE week = THIS_WEEK() |
| `priority_breakdown` | Tasks by priority | GROUP BY priority |

### 16.2 Statistics Storage

Statistics are computed on-demand and cached:
- User-level statistics are cached for 5 minutes
- Global statistics are cached for 15 minutes
- Cache is invalidated on task creation, completion, or status change

### 16.3 Statistics API

```yaml
GET /api/v1/tasks/statistics
  query_params:
    - period: today|week|month|year|all
    - goal_id: UUID (optional)
    - category: String (optional)
  response:
    completion_rate: number
    total_tasks: number
    completed_tasks: number
    overdue_tasks: number
    average_completion_time: number
    streak_days: number
    focus_time_minutes: number
    priority_breakdown: object
    category_breakdown: object
```

---

## 17. Domain Events

### 17.1 Event Catalog

The Task Engine publishes the following domain events:

| Event | Trigger | Payload |
|-------|---------|---------|
| `TaskCreated` | New task created | task_id, user_id, goal_id, title |
| `TaskUpdated` | Task fields modified | task_id, changed_fields |
| `TaskActivated` | Task moved from DRAFT to PENDING | task_id, user_id |
| `TaskStarted` | Task moved to IN_PROGRESS | task_id, user_id |
| `TaskPaused` | Task moved to PAUSED | task_id, user_id |
| `TaskBlocked` | Task moved to BLOCKED | task_id, blocking_dependency_ids |
| `TaskUnblocked` | Task moved from BLOCKED | task_id, resolved_dependency_ids |
| `TaskCompleted` | Task marked COMPLETED | task_id, user_id, completed_date, execution_type |
| `TaskFailed` | Task marked FAILED | task_id, user_id, failure_reason |
| `TaskCancelled` | Task marked CANCELLED | task_id, user_id, cancellation_reason |
| `TaskArchived` | Task marked ARCHIVED | task_id, user_id |
| `TaskDeleted` | Task soft-deleted | task_id, user_id |
| `TaskDependencyCreated` | New dependency created | task_id, depends_on_task_id, dependency_type |
| `TaskDependencyRemoved` | Dependency removed | task_id, depends_on_task_id |
| `TaskDependencyResolved` | Dependency resolved by completion | task_id, depends_on_task_id |
| `TaskDependencyCycleDetected` | Circular dependency prevented | task_ids_in_cycle |
| `TaskSubtaskCreated` | New subtask created | parent_task_id, subtask_id |
| `TaskSubtaskCompleted` | Subtask completed | parent_task_id, subtask_id |
| `TaskReminderDue` | Reminder time reached | task_id, user_id, due_date |
| `TaskRecurrenceGenerated` | New recurring instance created | original_task_id, new_task_id, occurrence_number |
| `TimeEntryStarted` | Focus session started | task_id, user_id, start_time |
| `TimeEntryStopped` | Focus session ended | task_id, user_id, end_time, duration_minutes |
| `TaskPriorityChanged` | Task priority modified | task_id, old_priority, new_priority |

### 17.2 Event Schema

All events follow a standard envelope:

```json
{
  "event_id": "uuid",
  "event_type": "TaskCompleted",
  "event_version": "1.0",
  "timestamp": "2026-08-07T10:00:00Z",
  "source": "task-engine",
  "payload": {
    "task_id": "uuid",
    "user_id": "uuid"
  },
  "metadata": {
    "correlation_id": "uuid",
    "causation_id": "uuid"
  }
}
```

---

## 18. API Design

### 18.1 REST Endpoints

```
TASKS
POST   /api/v1/tasks                    Create task
GET    /api/v1/tasks                    List tasks (with filters)
GET    /api/v1/tasks/:id                Get task by ID
PATCH  /api/v1/tasks/:id                Update task
DELETE /api/v1/tasks/:id                Delete task (soft)
POST   /api/v1/tasks/:id/complete       Complete task
POST   /api/v1/tasks/:id/fail           Fail task
POST   /api/v1/tasks/:id/cancel         Cancel task
POST   /api/v1/tasks/:id/archive        Archive task
POST   /api/v1/tasks/:id/restore        Restore from deleted/archived

SUBTASKS
POST   /api/v1/tasks/:id/subtasks       Create subtask
GET    /api/v1/tasks/:id/subtasks       List subtasks
DELETE /api/v1/tasks/:id/subtasks/:subtask_id  Delete subtask

DEPENDENCIES
POST   /api/v1/tasks/:id/dependencies   Add dependency
DELETE /api/v1/tasks/:id/dependencies/:depends_on_id  Remove dependency
GET    /api/v1/tasks/:id/dependencies   List dependencies
GET    /api/v1/tasks/:id/dependents     List tasks depending on this task

TIME ENTRIES
POST   /api/v1/tasks/:id/time-entries   Start time entry
PATCH  /api/v1/tasks/:id/time-entries/:entry_id  Stop time entry
GET    /api/v1/tasks/:id/time-entries   List time entries
DELETE /api/v1/tasks/:id/time-entries/:entry_id  Delete time entry

STATISTICS
GET    /api/v1/tasks/statistics         Get task statistics

RECURRING
POST   /api/v1/tasks/:id/recurrence     Configure recurrence
PATCH  /api/v1/tasks/:id/recurrence     Update recurrence
DELETE /api/v1/tasks/:id/recurrence     Remove recurrence

ATTACHMENTS
POST   /api/v1/tasks/:id/attachments    Add attachment
DELETE /api/v1/tasks/:id/attachments/:attachment_id  Remove attachment
GET    /api/v1/tasks/:id/attachments    List attachments
```

### 18.2 Query Parameters

```yaml
GET /api/v1/tasks:
  query_params:
    status: String[]        # Filter by status (comma-separated)
    priority: String[]      # Filter by priority
    category: String[]      # Filter by category
    goal_id: UUID           # Filter by goal
    parent_task_id: UUID    # Filter by parent task (null for top-level)
    due_before: ISO8601     # Due before this date
    due_after: ISO8601      # Due after this date
    tags: String[]          # Filter by tags
    execution_type: String  # Filter by execution type
    search: String          # Full-text search on title and description
    sort_by: String         # created_at, due_date, priority, status
    sort_order: String      # asc, desc
    page: Integer           # Page number (default 1)
    limit: Integer          # Items per page (default 20, max 100)
```

### 18.3 Error Codes

| HTTP Status | Code | Description |
|-------------|------|-------------|
| 400 | `INVALID_STATUS_TRANSITION` | Cannot transition from current status to target status |
| 400 | `CIRCULAR_DEPENDENCY` | Dependency would create a cycle |
| 400 | `SELF_DEPENDENCY` | Task cannot depend on itself |
| 400 | `DUPLICATE_DEPENDENCY` | Dependency already exists |
| 400 | `INVALID_DEPENDENCY` | Dependency target is invalid (archived, different user) |
| 400 | `PARENT_TASK_REQUIRED` | Subtask requires valid parent task |
| 400 | `SUBTASK_LIMIT_EXCEEDED` | Maximum subtask depth or count exceeded |
| 400 | `INVALID_EXECUTION_TYPE` | Execution type is not supported |
| 400 | `COMPLETION_CRITERIA_NOT_MET` | Task does not meet completion criteria |
| 404 | `TASK_NOT_FOUND` | Task does not exist |
| 404 | `DEPENDENCY_NOT_FOUND` | Dependency does not exist |
| 409 | `TASK_ALREADY_COMPLETED` | Task is already completed |
| 409 | `TASK_ALREADY_DELETED` | Task is already deleted |
| 409 | `RECURRENCE_ACTIVE` | Cannot modify task with active recurrence |
| 422 | `VALIDATION_ERROR` | General validation failure |

---

## 19. Security and Authorization

### 19.1 Ownership Model

- Every task is owned by exactly one user (`user_id`)
- Users can only access their own tasks
- Tasks with `visibility = FRIENDS` can be viewed by friends (future feature)
- Tasks with `visibility = PUBLIC` can be viewed by anyone (future feature)

### 19.2 Authorization Rules

| Operation | Required Permission |
|-----------|---------------------|
| Create task | Authenticated user |
| Read task | Owner or shared visibility |
| Update task | Owner |
| Delete task | Owner |
| Complete task | Owner |
| Add dependency | Owner of both tasks |
| Remove dependency | Owner of both tasks |
| Add time entry | Owner |
| Start timer | Owner |

### 19.3 Data Isolation

- All queries must filter by `user_id` and `deleted_at IS NULL`
- Cross-user dependency checks prevent unauthorized access
- Soft delete ensures data recovery while hiding from normal queries

---

## 20. Performance Considerations

### 20.1 Query Optimization

- All user-facing queries filter on `(user_id, deleted_at)` composite index
- Pagination is required for task lists (max 100 items per page)
- Heavy aggregations (statistics) use cached results
- Full-text search uses PostgreSQL `GIN` index on `title` and `description`

### 20.2 N+1 Query Prevention

- Subtask trees are loaded with recursive CTEs or batch loading
- Dependencies are loaded in batch with task data
- Time entry aggregates are pre-computed or cached

### 20.3 Scalability

- Tasks table is partitioned by `user_id` for very large deployments
- Time entries table is partitioned by `created_at` for archival
- Event publishing uses async message queue to avoid blocking API responses

---

## 21. Monitoring and Observability

### 21.1 Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `task.created.count` | Counter | Total tasks created |
| `task.completed.count` | Counter | Total tasks completed |
| `task.completion.rate` | Gauge | Current completion rate |
| `task.dependency.cycle.detected` | Counter | Circular dependency attempts |
| `task.transition.duration` | Histogram | Time spent in state transitions |
| `task.time_entry.duration` | Histogram | Duration of time entries |
| `task.recurrence.generated` | Counter | Recurring tasks generated |

### 21.2 Logging

- All state transitions are logged with before/after state
- Dependency resolution attempts are logged
- Execution provider actions are logged
- Error conditions include full context for debugging

### 21.3 Alerting

- Alert if task creation rate drops >50% (possible service issue)
- Alert if dependency cycle detection rate spikes (possible bug or abuse)
- Alert if time entry aggregation latency exceeds 500ms

---

## 22. Migration and Evolution

### 22.1 Schema Migrations

- All schema changes use versioned migrations
- Migrations are backward compatible
- Zero-downtime deployments are required

### 22.2 Feature Flags

New features are gated behind feature flags:

```yaml
flags:
  task_attachments_enabled: false  # Enable attachment support
  task_dependencies_enabled: true  # Enable dependency management
  task_recurrence_enabled: true    # Enable recurring tasks
  task_time_tracking_enabled: true # Enable time tracking
  task_statistics_enabled: true    # Enable statistics computation
  task_approval_enabled: false     # Enable approval workflow
```

### 22.3 Deprecation Policy

- Deprecated features are marked in API responses with `X-Deprecated: true` header
- Deprecated fields are removed only after 6 months notice
- Migration guides are provided for breaking changes

---

## 23. Testing Strategy

### 23.1 Unit Tests

- Test each state transition independently
- Test cycle detection algorithm with known graphs
- Test each Execution Provider in isolation
- Test time entry aggregation logic
- Test recurrence generation with edge cases

### 23.2 Integration Tests

- Test full task lifecycle from creation to archival
- Test dependency resolution with multiple tasks
- Test recurring task generation over multiple periods
- Test cross-engine event handling

### 23.3 Contract Tests

- Verify API response schemas against OpenAPI spec
- Verify event schemas against event catalog
- Verify database schema matches design document

### 23.4 Performance Tests

- Load test task creation (target: 1000 tasks/second)
- Load test task listing with filters (target: 500 requests/second)
- Load test statistics computation (target: 100 requests/second)
- Load test dependency resolution (target: 1000 operations/second)

---

## 24. Future Considerations

### 24.1 Planned Features

- AI-powered task breakdown (decompose large tasks into subtasks)
- Smart scheduling (suggest optimal task order based on energy levels)
- Task templates (pre-defined task structures)
- Bulk operations (mass complete, reschedule, etc.)
- Task sharing and collaboration
- Task import/export (CSV, JSON, iCal)
- Voice task creation
- Image attachment OCR for evidence

### 24.2 Scalability Enhancements

- Shard tasks by user_id for horizontal scaling
- Archive completed tasks to cold storage after 1 year
- Implement read replicas for analytics queries
- Add task versioning for audit trails

### 24.3 Integration Opportunities

- Calendar sync (two-way with Google Calendar, Outlook)
- Project management tools (Jira, Asana, Linear)
- Development tools (GitHub issues, GitLab tasks)
- Communication tools (Slack, Discord task commands)

---

## Appendix A: Glossary

| Term | Definition |
|------|------------|
| Task | An executable action unit in THE SYSTEM |
| Subtask | A child task that contributes to a parent task's completion |
| Dependency | A prerequisite relationship between tasks |
| Execution Type | The mode of task completion (boolean, checklist, timer, etc.) |
| Execution Provider | Pluggable logic that manages task execution for a specific type |
| Execution State | Structured JSON data tracking the runtime state of an execution type |
| Focus Session | A dedicated time period spent working on a task |
| Recurring Task | A task that repeats on a schedule |
| Soft Delete | Marking a record as deleted without removing it from the database |
| Domain Event | An immutable record of something that happened in the system |

---

## Appendix B: References

- THE SYSTEM Architecture Overview
- Goal Engine Design Document
- XP Engine Design Document
- Memory Engine Design Document
- Analytics Engine Design Document
- Notification Engine Design Document
- AI Engine Design Document
- PostgreSQL JSONB Documentation
- Domain-Driven Design by Eric Evans


