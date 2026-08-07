# Goal Module

## Purpose

The Goal Engine is the heart of THE SYSTEM. It is the single source of truth for all user objectives, achievements, projects, missions, learning objectives, and personal aspirations. Every long-term achievement, project, mission, learning objective, and personal objective originates here.

## Responsibilities

### Owned by Goal Engine

- Goal entities and lifecycle management
- Goal progress tracking and calculation
- Goal status transitions (DRAFT, ACTIVE, PAUSED, COMPLETED, FAILED, ARCHIVED)
- Goal priority and difficulty management
- Goal categorization and tagging
- Goal deadlines and archiving
- Goal milestones and their ordering
- Goal statistics and reporting
- Completion strategy management

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

## Database

### goals table

- `id` UUID PRIMARY KEY
- `user_id` UUID NOT NULL (FK to users)
- `title` VARCHAR(255) NOT NULL
- `description` TEXT
- `category` VARCHAR(50)
- `priority` VARCHAR(20) NOT NULL DEFAULT 'NORMAL'
- `difficulty` VARCHAR(20)
- `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
- `visibility` VARCHAR(20) NOT NULL DEFAULT 'PRIVATE'
- `estimated_xp` INTEGER NOT NULL DEFAULT 0
- `current_progress` INTEGER NOT NULL DEFAULT 0
- `completion_percentage` DOUBLE PRECISION NOT NULL DEFAULT 0.0
- `target_date` TIMESTAMP NULL
- `completed_date` TIMESTAMP NULL
- `archived_date` TIMESTAMP NULL
- `completion_strategy` VARCHAR(50)
- `tags` JSONB NULL
- `custom_metadata` JSONB NULL
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL
- `created_by` UUID
- `updated_by` UUID
- `deleted_at` TIMESTAMP NULL

### goal_milestones table

- `id` UUID PRIMARY KEY
- `goal_id` UUID NOT NULL (FK to goals)
- `title` VARCHAR(255) NOT NULL
- `description` TEXT
- `display_order` INTEGER NOT NULL DEFAULT 0
- `is_completed` BOOLEAN NOT NULL DEFAULT FALSE
- `completed_date` TIMESTAMP NULL
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL
- `created_by` UUID
- `updated_by` UUID
- `deleted_at` TIMESTAMP NULL

### Indexes

- `idx_goals_user_id` on goals(user_id)
- `idx_goals_user_status` on goals(user_id, status) WHERE deleted_at IS NULL
- `idx_goals_status` on goals(status) WHERE deleted_at IS NULL
- `idx_goals_priority` on goals(priority) WHERE deleted_at IS NULL
- `idx_goals_category` on goals(category) WHERE deleted_at IS NULL
- `idx_goals_target_date` on goals(target_date) WHERE deleted_at IS NULL AND target_date IS NOT NULL
- `idx_goal_milestones_goal_id` on goal_milestones(goal_id)
- `idx_goal_milestones_display_order` on goal_milestones(goal_id, display_order)
- `uq_goal_milestones_goal_display_order` unique on goal_milestones(goal_id, display_order) WHERE deleted_at IS NULL

## Entities

### Goal

Core entity representing a user's goal with lifecycle state, progress, and metadata.

### GoalMilestone

Sub-entity representing a checkpoint within a goal with explicit ordering.

## Goal Lifecycle

```
DRAFT → ACTIVE → PAUSED → ACTIVE
  ↓           ↓         ↓
ARCHIVED  COMPLETED  ARCHIVED
           ↓
          FAILED → ACTIVE (retry)
```

## Completion Strategies

- MANUAL: User marks goal as complete
- TASK_BASED: Complete X of Y linked tasks
- XP_BASED: Reach target XP
- MILESTONE_BASED: Complete all milestones
- PERCENTAGE: Manual percentage target
- CUSTOM: User-defined condition

## API Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | /goals | Create goal | User |
| GET | /goals | List goals | User |
| GET | /goals/{id} | Get goal details | User |
| PUT | /goals/{id} | Update goal | User |
| DELETE | /goals/{id} | Delete goal | User |
| POST | /goals/{id}/start | Start goal | User |
| POST | /goals/{id}/pause | Pause goal | User |
| POST | /goals/{id}/resume | Resume goal | User |
| POST | /goals/{id}/complete | Complete goal | User |
| POST | /goals/{id}/fail | Fail goal | User |
| POST | /goals/{id}/archive | Archive goal | User |
| PUT | /goals/{id}/progress | Update progress | User |
| GET | /goals/{id}/milestones | List milestones | User |
| POST | /goals/{id}/milestones | Create milestone | User |
| PUT | /goals/{id}/milestones/{mid} | Update milestone | User |
| POST | /goals/{id}/milestones/{mid}/complete | Complete milestone | User |
| DELETE | /goals/{id}/milestones/{mid} | Delete milestone | User |
| GET | /goals/statistics | Get goal statistics | User |
| GET | /goals/category/{category} | Get goals by category | User |
| GET | /goals/status/{status} | Get goals by status | User |

## Business Rules

1. Users can only manage their own goals
2. Goal title is required (1-255 characters)
3. Goals start in DRAFT status
4. Only ACTIVE goals can be completed
5. Completing a goal sets completion_percentage to 100%
6. Milestones are ordered by display_order within a goal
7. Archived goals cannot be resumed (must create new goal)
8. Failed goals can be retried (status → ACTIVE)

## Events

- GoalCreatedEvent
- GoalUpdatedEvent
- GoalStartedEvent
- GoalPausedEvent
- GoalCompletedEvent
- GoalArchivedEvent
- GoalDeletedEvent
- GoalProgressUpdatedEvent

## Future Integration

- Task Engine: Automatic progress from completed tasks
- XP Engine: XP awards upon goal completion
- Analytics Engine: Goal completion analytics
- Notification Engine: Deadline reminders and milestone celebrations
- AI Engine: Goal suggestions and recommendations

## Future Roadmap

- Goal dependencies (prerequisites)
- Goal templates
- Goal sharing and collaboration
- Goal reflections and reviews
- Advanced completion strategies
- Goal hierarchies (parent/child goals)
