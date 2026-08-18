# Analytics Module

## Purpose

The Analytics Engine provides per-user reporting across engines. It is a read-only reporting engine that derives all metrics on-demand from persisted data owned by other engines.

## Database Tables

The Analytics Engine owns no tables. It accesses data through service interfaces:
- `xp_accounts`, `xp_transactions`, `user_streak_history` (via XP Service)
- `tasks`, `task_time_entries` (via Task Service)
- `goals` (via Goal Service)
- `memories` (via Memory Service)
- `ai_interactions` (via AI Service)

## API Endpoints

- `GET /api/v1/analytics/overview` — Cross-domain per-user overview snapshot
- `GET /api/v1/analytics/trends?days=14` — Time-series trends over bounded look-back window (1–90 days)
- `GET /api/v1/analytics/ai-usage` — AI interaction usage reporting
- `GET /api/v1/analytics/memories` — Memory usage reporting

## Business Rules

- All endpoints require authentication
- User identity comes from the authenticated security context
- No client-provided userId
- `days` parameter defaults to 14, minimum 1, maximum 90
- Timezone-aware day bucketing via `UserTimezoneResolver`
- All series are zero-filled for the requested window

## Relationships

- Analytics → Task, Goal, XP, Memory, AI, User (one-directional)

## Events

No domain events. The engine is read-only.

## Future Roadmap

- Event-driven incremental read models once an event bus/outbox exists
- Engine-owned SQL aggregation when volumes demand it
