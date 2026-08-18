# Notification Engine Design

## Version
1.0.0

## Date
2026-08-17

## Status
Accepted

---

## 1. Purpose

Implement the Notification module (`modules/notification`) as a read-only, event-driven in-app notification system. The Notification Engine consumes domain events from existing engines and creates notification records that users can view, read, and dismiss.

## 2. Ownership

The Notification Engine owns:
- Notification records and their persistence
- Event-to-notification mapping logic
- Notification API and user-facing semantics
- Notification preferences consumption

The Notification Engine does NOT own:
- Task, Goal, XP, Streak, Achievement, Memory, or AI state
- Event production (publishing remains in owning engines)
- External delivery channels in v1

## 3. Non-goals

- No email, SMS, push, or WebSocket delivery in v1
- No external notification providers
- No notification batching or digest
- No notification templates beyond simple title/message
- No notification search or advanced filtering
- No notification categories beyond type enum

## 4. Architecture

```
Task Engine       Goal Engine       XP Engine       Memory Engine       AI Engine
     |                |                |                 |                 |
     ▼                ▼                ▼                 ▼                 ▼
 Domain Event      Domain Event     Domain Event      Domain Event      Domain Event
     |                |                |                 |                 |
     └────────────────┴────────────────┴─────────────────┴─────────────────┘
                              |
                              ▼
                   Notification Listeners
                   (event → notification mapping)
                              |
                              ▼
                   Notification Service
                              |
                              ▼
                   Notification Repository
                              |
                              ▼
                   In-App Notification
```

Notification listens to events; producing engines remain unaware of Notification implementation details.

## 5. Database Model

Flyway migration `V22__create_notifications_table.sql`:

```sql
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    related_entity_type VARCHAR(50) NULL,
    related_entity_id UUID NULL,
    metadata JSONB NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_created ON notifications(user_id, created_at) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON notifications(user_id, is_read) WHERE deleted_at IS NULL;
```

Fields:
- `id`: UUID primary key
- `user_id`: owner of the notification
- `type`: notification type enum value (TASK_COMPLETED, GOAL_COMPLETED, ACHIEVEMENT_UNLOCKED, LEVEL_UP, STREAK_MILESTONE)
- `title`: short display title
- `message`: human-readable message body
- `related_entity_type`: optional related entity type (TASK, GOAL, ACHIEVEMENT, LEVEL, STREAK)
- `related_entity_id`: optional related entity UUID
- `metadata`: optional JSONB for extra data (not sensitive)
- `is_read`: read state
- `read_at`: timestamp when marked read
- Audit fields: created_at, updated_at, created_by, updated_by, deleted_at

## 6. Notification Types

| Type | Value | Description |
|------|-------|-------------|
| TASK_COMPLETED | TASK_COMPLETED | A task was completed |
| GOAL_COMPLETED | GOAL_COMPLETED | A goal was completed |
| ACHIEVEMENT_UNLOCKED | ACHIEVEMENT_UNLOCKED | An achievement was unlocked |
| LEVEL_UP | LEVEL_UP | User reached a new level |
| STREAK_MILESTONE | STREAK_MILESTONE | User reached a streak milestone |

## 7. Event Sources

| Event | Notification Type | Condition |
|-------|-------------------|-----------|
| TaskCompletedEvent | TASK_COMPLETED | Always |
| GoalCompletedEvent | GOAL_COMPLETED | Always |
| AchievementUnlockedEvent | ACHIEVEMENT_UNLOCKED | Always |
| LevelUpEvent | LEVEL_UP | Always |
| StreakMilestoneReachedEvent | STREAK_MILESTONE | Always |

## 8. Event-to-Notification Mapping

Listeners create notifications from events. The mapping is straightforward:

- `TaskCompletedEvent` → title: "Task completed", message: "{title} completed", related to task
- `GoalCompletedEvent` → title: "Goal completed", message: "{difficulty} goal completed", related to goal
- `AchievementUnlockedEvent` → title: "Achievement unlocked", message: "{achievementCode} unlocked (+{xpReward} XP)", related to achievement
- `LevelUpEvent` → title: "Level up!", message: "You reached level {newLevel}", related to level
- `StreakMilestoneReachedEvent` → title: "Streak milestone!", message: "{milestone}-day streak reached", related to streak

## 9. API Contract

All endpoints require authentication. User identity comes from `SecurityUtils.getCurrentUserId()`. No `userId` is accepted from request parameters.

| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1/notifications | List notifications (paginated, unread first) |
| GET | /api/v1/notifications/{id} | Get single notification |
| PATCH | /api/v1/notifications/{id}/read | Mark notification as read |
| PATCH | /api/v1/notifications/read-all | Mark all notifications as read |
| DELETE | /api/v1/notifications/{id} | Delete/dismiss notification |

Response envelope follows the standard `ApiResponse` pattern.

## 10. Security

- All endpoints use the default-deny security configuration.
- `SecurityUtils.getCurrentUserId()` provides the caller identity.
- Every repository query filters by `user_id = :userId AND deleted_at IS NULL`.
- Cross-user access returns 404 (not 403) to avoid leaking existence.
- No sensitive event payloads are stored in `metadata`.

## 11. User Isolation

- Notifications are scoped exclusively by authenticated user.
- User A cannot read, modify, or delete User B's notifications.
- Unauthenticated requests receive 401.

## 12. Read/Unread Semantics

- `is_read` defaults to `false`.
- `read_at` is set when marked read.
- Listing defaults to showing all notifications (read and unread), ordered by `created_at DESC`.
- Unread count is derivable from the list response.

## 13. Lifecycle

1. Event is published by owning engine after persistence commit.
2. Notification listener receives the event.
3. Listener maps event to notification DTO/entity.
4. Service persists notification.
5. User views notifications via API.
6. User marks notifications as read or deletes them.
7. Soft delete is used; deleted notifications are excluded from queries.

## 14. Error Handling

- Listener failures are logged but do not affect the originating engine.
- Service exceptions propagate to the controller and return standard error envelopes.
- 404 for missing notifications or cross-user access.

## 15. Transaction/Event Behavior

Events are published within the originating engine's transaction. Spring `ApplicationEventPublisher` publishes synchronously by default. If the publishing transaction rolls back, the event is not delivered. This matches the existing project pattern (GoalTaskProgressListener, StreakEventListener, XpEventListener).

Notification creation is idempotent per source event using a composite key of `user_id`, `type`, `related_entity_type`, `related_entity_id`, and `deleted_at IS NULL`. Duplicate events do not create duplicate notifications.

## 16. Testing Strategy

- **Repository**: persistence, user scoping, unread filtering, ordering, soft delete
- **Service**: create from event, list, mark read, mark all read, delete, not found, cross-user isolation, idempotency
- **Listeners**: each supported event creates the correct notification; unsupported events are ignored
- **Controller**: authenticated access, unauthenticated → 401, validation, not found, cross-user access, ApiResponse envelope
- **Frontend**: lint and build

## 17. Frontend Strategy

- Add notification API client (`notificationApi.js`).
- Add notification page or panel.
- Show unread indicator.
- Support read/unread state, loading, empty, and error states.
- Follow existing UI conventions from Tasks, Goals, Memories, AI, Profile, Progress, Settings, Dashboard.

## 18. Future Delivery Channels

- Email, SMS, push, and external providers are deferred.
- The `metadata` field and type enum are designed to support future channels without schema changes.

## 19. Technical Debt

- No notification preferences engine in v1 beyond the existing `notification.enabled` and `xp.*Notifications` settings.
- No notification batching or digest.
- No read receipts or per-notification preferences.

## 20. Roadmap

- v1: in-app notifications, read/unread, listing, mark read, mark all read, delete
- Future: email digest, push notifications, per-type preferences, notification actions
