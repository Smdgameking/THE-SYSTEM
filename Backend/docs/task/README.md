# Task Engine

## Purpose

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

## Responsibilities

### Owned by Task Engine

- Task entities and lifecycle management
- Task priority management (LOW, NORMAL, HIGH, CRITICAL)
- Task scheduling (due dates, reminders, start dates)
- Task dependencies with cycle prevention
- Subtask hierarchy and completion rules
- Task recurrence configuration and generation
- Task completion evidence and verification
- Task time tracking (manual, timer, pomodoro, break)
- Execution types (BOOLEAN, CHECKLIST, TIMER, COUNT, PROGRESS, HABIT, APPROVAL, CUSTOM)
- Execution providers and execution state
- Task statistics and reporting
- Task visibility and sharing

### NOT Owned by Task Engine

- Goals (owned by Goal Engine)
- Goal progress calculation (owned by Goal Engine)
- XP calculation and awards (owned by XP Engine)
- Memory storage (owned by Memory Engine)
- Analytics computation (owned by Analytics Engine)
- Notifications (owned by Notification Engine)
- AI suggestions (owned by AI Engine)
- User authentication (owned by Auth Engine)
- User profiles (owned by User Engine)
- Settings (owned by Settings Engine)

## Database

### tasks table

- `id` UUID PRIMARY KEY
- `user_id` UUID NOT NULL (FK to users)
- `goal_id` UUID NULL (FK to goals)
- `parent_task_id` UUID NULL (FK to tasks, self-referential)
- `title` VARCHAR(255) NOT NULL
- `description` TEXT NULL
- `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
- `priority` VARCHAR(20) NOT NULL DEFAULT 'NORMAL'
- `execution_type` VARCHAR(20) NOT NULL DEFAULT 'BOOLEAN'
- `category` VARCHAR(50) NULL
- `estimated_duration` INTEGER NULL
- `actual_duration` INTEGER NULL
- `start_date` TIMESTAMP NULL
- `due_date` TIMESTAMP NULL
- `completed_date` TIMESTAMP NULL
- `reminder_date` TIMESTAMP NULL
- `is_recurring` BOOLEAN NOT NULL DEFAULT FALSE
- `recurring_config_id` UUID NULL
- `tags` JSONB NULL
- `attachments` JSONB NULL
- `notes` TEXT NULL
- `completion_evidence` JSONB NULL
- `execution_state` JSONB NULL
- `custom_metadata` JSONB NULL
- `visibility` VARCHAR(20) NOT NULL DEFAULT 'PRIVATE'
- `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- `created_by` UUID NULL
- `updated_by` UUID NULL
- `deleted_at` TIMESTAMP NULL

### task_dependencies table

- `id` UUID PRIMARY KEY
- `task_id` UUID NOT NULL (FK to tasks)
- `depends_on_task_id` UUID NOT NULL (FK to tasks)
- `dependency_type` VARCHAR(20) NOT NULL DEFAULT 'BLOCKS'
- `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING'
- `resolved_date` TIMESTAMP NULL
- `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- `created_by` UUID NULL
- `deleted_at` TIMESTAMP NULL
- UNIQUE (task_id, depends_on_task_id)

### task_time_entries table

- `id` UUID PRIMARY KEY
- `task_id` UUID NOT NULL (FK to tasks)
- `user_id` UUID NOT NULL (FK to users)
- `start_time` TIMESTAMP NOT NULL
- `end_time` TIMESTAMP NULL
- `duration_minutes` INTEGER NULL
- `entry_type` VARCHAR(20) NOT NULL DEFAULT 'MANUAL'
- `notes` TEXT NULL
- `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- `created_by` UUID NULL
- `deleted_at` TIMESTAMP NULL

### recurring_task_configs table

- `id` UUID PRIMARY KEY
- `task_id` UUID NOT NULL (FK to tasks)
- `frequency` VARCHAR(20) NOT NULL
- `interval_value` INTEGER NOT NULL DEFAULT 1
- `cron_expression` VARCHAR(100) NULL
- `days_of_week` INTEGER[] NULL
- `day_of_month` INTEGER NULL
- `month` INTEGER NULL
- `exception_dates` DATE[] NULL
- `end_date` TIMESTAMP NULL
- `max_occurrences` INTEGER NULL
- `occurrence_count` INTEGER NOT NULL DEFAULT 0
- `is_active` BOOLEAN NOT NULL DEFAULT TRUE
- `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- `created_by` UUID NULL

### Indexes

- `idx_tasks_user_id` on tasks(user_id)
- `idx_tasks_user_status` on tasks(user_id, status) WHERE deleted_at IS NULL
- `idx_tasks_user_due_date` on tasks(user_id, due_date) WHERE deleted_at IS NULL AND due_date IS NOT NULL
- `idx_tasks_status` on tasks(status) WHERE deleted_at IS NULL
- `idx_tasks_priority` on tasks(priority) WHERE deleted_at IS NULL
- `idx_tasks_category` on tasks(category) WHERE deleted_at IS NULL
- `idx_tasks_due_date` on tasks(due_date) WHERE deleted_at IS NULL AND due_date IS NOT NULL
- `idx_tasks_goal_id` on tasks(goal_id) WHERE deleted_at IS NULL AND goal_id IS NOT NULL
- `idx_tasks_parent_task_id` on tasks(parent_task_id) WHERE deleted_at IS NULL AND parent_task_id IS NOT NULL
- `idx_tasks_is_recurring` on tasks(is_recurring) WHERE deleted_at IS NULL
- `idx_tasks_execution_type` on tasks(execution_type) WHERE deleted_at IS NULL
- `idx_task_dependencies_task_id` on task_dependencies(task_id)
- `idx_task_dependencies_depends_on` on task_dependencies(depends_on_task_id)
- `idx_task_time_entries_task_id` on task_time_entries(task_id)
- `idx_task_time_entries_user_id` on task_time_entries(user_id)
- `idx_recurring_task_configs_task_id` on recurring_task_configs(task_id)
- `idx_recurring_task_configs_is_active` on recurring_task_configs(is_active) WHERE is_active = TRUE
- `idx_tasks_deleted_at` on tasks(deleted_at)
- `idx_task_dependencies_deleted_at` on task_dependencies(deleted_at)
- `idx_task_time_entries_deleted_at` on task_time_entries(deleted_at)

### Relationships

- User 1:N tasks (ON DELETE CASCADE)
- Goal 1:N tasks (ON DELETE SET NULL)
- Task 1:N subtasks via parent_task_id (ON DELETE CASCADE)
- Task 1:N task_dependencies as task_id (ON DELETE CASCADE)
- Task 1:N task_dependencies as depends_on_task_id (ON DELETE CASCADE)
- Task 1:N task_time_entries (ON DELETE CASCADE)
- User 1:N task_time_entries (ON DELETE CASCADE)
- Task 1:1 recurring_task_configs (ON DELETE CASCADE)

## Execution Types

The Task Engine supports eight execution types, each mapped to a dedicated provider:

| Type | Code | Description |
|------|------|-------------|
| Boolean | BOOLEAN | Simple yes/no completion |
| Checklist | CHECKLIST | List of items to complete |
| Timer | TIMER | Time-based task with focus sessions |
| Count | COUNT | Repeat N times (e.g., 10 pushups) |
| Progress | PROGRESS | Percentage-based progress (0-100%) |
| Habit | HABIT | Recurring behavior tracking |
| Approval | APPROVAL | Requires approval from another user |
| Custom | CUSTOM | Extensible for AI or custom logic |

## Execution Providers

The Task Engine uses the Execution Provider pattern defined in ADR-0007.

### Core Principles

1. **Task Engine owns execution behavior**: No external engine may implement task execution logic
2. **Each execution type has a dedicated provider**: Encapsulates validation, progress calculation, and completion rules
3. **Execution State is structured JSONB**: Not a simple flag, but a complete runtime representation
4. **Providers are pluggable**: New execution types added via registry, no core logic changes
5. **Task Engine coordinates**: Owns lifecycle, delegates execution to providers

### Provider Interface

```java
public interface TaskExecutionProvider {
    TaskExecutionState initialize(Task task);
    TaskExecutionState calculateProgress(Task task, TaskExecutionState state);
    boolean isComplete(Task task, TaskExecutionState state);
    void validate(Task task, TaskExecutionState state);
    TaskExecutionType getType();
}
```

### Registry

All providers are registered via `TaskExecutionProviderRegistry`:

- BooleanExecutionProvider
- ChecklistExecutionProvider
- TimerExecutionProvider
- CountExecutionProvider
- ProgressExecutionProvider
- HabitExecutionProvider
- ApprovalExecutionProvider
- CustomExecutionProvider

## Execution State

Execution state is stored as JSONB in the `execution_state` column. Each execution type defines its own schema:

**BOOLEAN**
```json
{ "completed": true }
```

**CHECKLIST**
```json
{ "items": [...], "completedItems": 1, "totalItems": 2 }
```

**TIMER**
```json
{ "targetMinutes": 120, "completedMinutes": 95, "sessions": 4, "paused": false }
```

**COUNT**
```json
{ "target": 100, "completed": 62, "unit": "pages" }
```

**PROGRESS**
```json
{ "percentage": 67 }
```

**HABIT**
```json
{ "currentStreak": 14, "bestStreak": 30, "lastCompletion": "...", "frequency": "DAILY" }
```

**APPROVAL**
```json
{ "submitted": true, "approved": false, "approvedBy": null }
```

**CUSTOM**
```json
{ "config": {}, "data": {} }
```

Execution state is extensible via JSONB schema evolution without requiring database migrations. The `custom_metadata` column provides additional engine-specific extension space.

## Lifecycle

### Task Statuses

| Status | Description | Allowed Transitions |
|--------|-------------|---------------------|
| DRAFT | Task is being created, not yet ready for execution | PENDING, CANCELLED |
| PENDING | Task is queued for execution, dependencies may be unresolved | IN_PROGRESS, CANCELLED, ARCHIVED |
| IN_PROGRESS | Task is actively being worked on | PAUSED, BLOCKED, COMPLETED, FAILED |
| BLOCKED | Task cannot proceed due to unresolved dependencies | IN_PROGRESS, CANCELLED |
| PAUSED | Task execution is temporarily halted | IN_PROGRESS, CANCELLED |
| COMPLETED | Task has been successfully finished | ARCHIVED |
| FAILED | Task execution failed | PENDING, CANCELLED |
| CANCELLED | Task was intentionally abandoned | ARCHIVED |
| ARCHIVED | Task is finalized and stored for history | (terminal state) |

### State Machine

```
DRAFT → PENDING → IN_PROGRESS → COMPLETED → ARCHIVED
   ↓           ↓            ↓           ↑
 CANCELLED   BLOCKED      FAILED      │
              ↓            ↓           │
           IN_PROGRESS   PENDING ─────┘
              ↑            ↓
           CANCELLED   CANCELLED → ARCHIVED
```

### Transition Side Effects

| Transition | Side Effects |
|------------|-------------|
| DRAFT → PENDING | Validate required fields, set start_date if not set, publish TaskActivated event |
| PENDING → IN_PROGRESS | Set actual_duration start tracking, publish TaskStarted event |
| IN_PROGRESS → BLOCKED | Identify blocking dependencies, publish TaskBlocked event |
| BLOCKED → IN_PROGRESS | Verify dependencies resolved, publish TaskUnblocked event |
| IN_PROGRESS → COMPLETED | Set completed_date, calculate actual_duration, aggregate time entries, publish TaskCompleted event |
| COMPLETED → ARCHIVED | Mark as read-only, publish TaskArchived event |
| IN_PROGRESS → FAILED | Record failure reason, publish TaskFailed event |
| FAILED → PENDING | Reset execution state, allow retry |
| Any → CANCELLED | Record cancellation reason, release resources, publish TaskCancelled event |

## Dependencies

### Dependency Types

| Type | Code | Description |
|------|------|-------------|
| Blocks | BLOCKS | Task B cannot start until Task A is complete |
| Related | RELATED | Tasks are related but do not block each other |

### Dependency Statuses

- PENDING
- RESOLVED
- FAILED
- CANCELLED

### Cycle Prevention

The Task Engine prevents circular dependencies using topological sort with cycle detection (Kahn's algorithm):

1. Build adjacency list from task_dependencies
2. Perform Kahn's algorithm for topological sort
3. If cycle detected, reject the dependency creation and publish TaskDependencyCycleDetected event
4. If no cycle, create the dependency, update BLOCKED status, publish TaskDependencyCreated event

Complexity: O(V + E)

### Dependency Resolution

When a task's dependency is completed:
1. Check if all dependencies for the blocked task are resolved
2. If all resolved, transition task from BLOCKED to PENDING
3. Publish TaskDependencyResolved event

When a task's dependency is cancelled or fails:
1. Remove the dependency or mark as unresolved
2. Re-evaluate the blocked task's status

## Recurrence

### Configuration

Recurring tasks use a separate `recurring_task_configs` table:

- `frequency`: DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM
- `interval_value`: Repeat every N units (e.g., every 2 weeks)
- `cron_expression`: For CUSTOM frequency, standard cron syntax
- `days_of_week`: For WEEKLY: which days (0=Sunday, 6=Saturday)
- `day_of_month`: For MONTHLY: which day of month
- `month`: For YEARLY: which month (1-12)
- `exception_dates`: Dates to skip (holidays, vacations)
- `end_date`: When recurrence stops generating tasks
- `max_occurrences`: Maximum number of generated tasks
- `occurrence_count`: Counter of generated tasks
- `is_active`: Whether recurrence is currently active

### Generation Rules

New task instances are generated when a recurring task is completed:

- `is_active` must be TRUE
- `end_date` must be NULL or in the future
- `occurrence_count` must be less than `max_occurrences` (if set)
- Create new task with same title, description, priority, etc.
- Reset status to PENDING
- Clear completion fields
- Increment occurrence_count
- Calculate next due_date based on frequency
- Exclude exception_dates from next occurrence calculation
- Publish TaskRecurrenceGenerated event

### Termination

Recurrence stops when ANY of the following conditions are met:
- `is_active` is set to FALSE
- `end_date` has passed
- `occurrence_count` >= `max_occurrences`
- The parent task is deleted
- The parent task is archived

## Time Tracking

### Entry Types

| Type | Code | Description |
|------|------|-------------|
| Manual | MANUAL | User manually logs time |
| Timer | TIMER | Automatic timer during focus session |
| Pomodoro | POMODORO | Pomodoro technique intervals |
| Break | BREAK | Break time between sessions |

### Time Entry Model

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

### Time Aggregation

- `task.actual_duration` is the sum of all `duration_minutes` from `task_time_entries` for that task
- Updated automatically when time entries are created, updated, or deleted
- Only non-deleted time entries are counted

### Focus Sessions

Focus sessions are a specialized time tracking feature:
- User starts a focus session on a task
- Timer runs automatically
- Session ends when user stops or task is completed
- Focus session data is stored in `execution_state` for TIMER type tasks
- Multiple focus sessions can be active on different tasks (but not the same task concurrently)

## Events

The Task Engine publishes the following domain events:

- `TaskCreatedEvent` - fired when a new task is created
- `TaskUpdatedEvent` - fired when task fields are modified
- `TaskActivatedEvent` - fired when task moves from DRAFT to PENDING
- `TaskStartedEvent` - fired when task moves to IN_PROGRESS
- `TaskPausedEvent` - fired when task moves to PAUSED
- `TaskBlockedEvent` - fired when task moves to BLOCKED
- `TaskUnblockedEvent` - fired when task moves from BLOCKED
- `TaskCompletedEvent` - fired when task is marked COMPLETED
- `TaskFailedEvent` - fired when task is marked FAILED
- `TaskCancelledEvent` - fired when task is marked CANCELLED
- `TaskArchivedEvent` - fired when task is marked ARCHIVED
- `TaskDeletedEvent` - fired on soft delete
- `TaskDependencyCreatedEvent` - fired when a new dependency is created
- `TaskDependencyRemovedEvent` - fired when a dependency is removed
- `TaskDependencyResolvedEvent` - fired when a dependency is resolved by completion
- `TaskDependencyCycleDetectedEvent` - fired when a circular dependency is prevented
- `TaskSubtaskCreatedEvent` - fired when a new subtask is created
- `TaskSubtaskCompletedEvent` - fired when a subtask is completed
- `TaskReminderDueEvent` - fired when reminder time is reached
- `TaskRecurrenceGeneratedEvent` - fired when a new recurring instance is created
- `TimeEntryStartedEvent` - fired when a focus session is started
- `TimeEntryStoppedEvent` - fired when a focus session is ended
- `TaskPriorityChangedEvent` - fired when task priority is modified

## Future Integrations

- Goal Engine: Automatic progress updates from completed tasks
- XP Engine: XP awards upon task completion based on execution type and state
- Analytics Engine: Task completion analytics, time tracking reports, dependency analysis
- Notification Engine: Deadline reminders and task status notifications
- AI Engine: Task suggestions, duration estimation, overload detection, automatic task breakdown
- Memory Engine: Task completion signals and learning patterns
- Calendar Engine: Two-way sync with Google Calendar and Outlook
- Project Management Tools: Jira, Asana, Linear integration
- Development Tools: GitHub issues, GitLab tasks integration
- Communication Tools: Slack, Discord task commands

## Future Roadmap

- Task templates
- Bulk operations (mass complete, reschedule)
- Task sharing and collaboration
- Task import/export (CSV, JSON, iCal)
- Voice task creation
- Smart scheduling based on energy levels
- Task versioning for audit trails
- Image attachment OCR for evidence
