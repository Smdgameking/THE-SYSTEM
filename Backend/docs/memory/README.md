# Memory Engine

## Purpose

The Memory Engine is the long-term knowledge store of THE SYSTEM. While Tasks define WHAT the user does and Goals define WHY, the Memory Engine captures and persists WHAT the user learns, prefers, and signals to the system over time.

The Memory Engine provides:
- Structured storage of user memories (notes, facts, preferences, insights, signals)
- Importance-based prioritization of retained knowledge
- Source attribution for every memory (manual, task, goal, AI)
- Keyword search across memory title and content
- Filtering by type, importance, and source
- Full CRUD lifecycle with soft delete
- Domain event publishing for integration with other engines
- Extension points for the future AI Engine

## Responsibilities

### Owned by Memory Engine

- Memory entities and lifecycle management
- Memory type classification (NOTE, FACT, PREFERENCE, INSIGHT, SIGNAL)
- Memory importance ranking (LOW, NORMAL, HIGH, CRITICAL)
- Memory source attribution (MANUAL, TASK, GOAL, AI)
- Tag-based organization
- Keyword search and filtering
- Custom metadata storage (reserved for engine-specific and AI payloads)

### NOT Owned by Memory Engine

- Tasks (owned by Task Engine)
- Goals (owned by Goal Engine)
- XP calculation and awards (owned by XP Engine)
- Analytics computation (owned by Analytics Engine)
- Notifications (owned by Notification Engine)
- AI generation of memories (owned by AI Engine)
- User authentication (owned by Auth Engine)
- User profiles (owned by User Engine)
- Settings (owned by Settings Engine)

## Database

### memories table

- `id` UUID PRIMARY KEY
- `user_id` UUID NOT NULL (FK to users, ON DELETE CASCADE)
- `title` VARCHAR(255) NOT NULL
- `content` TEXT NOT NULL
- `type` VARCHAR(20) NOT NULL DEFAULT 'NOTE' (CHECK in NOTE, FACT, PREFERENCE, INSIGHT, SIGNAL)
- `importance` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' (CHECK in LOW, NORMAL, HIGH, CRITICAL)
- `source` VARCHAR(20) NOT NULL DEFAULT 'MANUAL' (CHECK in MANUAL, TASK, GOAL, AI)
- `source_id` UUID NULL (opaque reference to originating entity, no FK)
- `tags` JSONB NULL
- `custom_metadata` JSONB NULL
- `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- `created_by` UUID NULL
- `updated_by` UUID NULL
- `deleted_at` TIMESTAMP NULL

### Indexes

- `idx_memories_user_id` on memories(user_id)
- `idx_memories_user_type` on memories(user_id, type) WHERE deleted_at IS NULL
- `idx_memories_user_importance` on memories(user_id, importance) WHERE deleted_at IS NULL
- `idx_memories_user_source` on memories(user_id, source) WHERE deleted_at IS NULL
- `idx_memories_source_id` on memories(source_id) WHERE deleted_at IS NULL AND source_id IS NOT NULL
- `idx_memories_deleted_at` on memories(deleted_at)

### Relationships

- User 1:N memories (ON DELETE CASCADE)
- `source_id` is intentionally opaque: it points to entities owned by other engines (tasks, goals) without a database FK, keeping module boundaries clean and enabling eventual module extraction

## Memory Model

### Types

| Type | Code | Description |
|------|------|-------------|
| Note | NOTE | Free-form note or record |
| Fact | FACT | Stated fact about the user or their world |
| Preference | PREFERENCE | User preference affecting future behavior |
| Insight | INSIGHT | Learned observation or conclusion |
| Signal | SIGNAL | Event/state signal captured for later processing |

### Importance Levels

| Level | Code | Description |
|-------|------|-------------|
| Low | LOW | Minor, transient knowledge |
| Normal | NORMAL | Default level for user-created memories |
| High | HIGH | Important knowledge with lasting value |
| Critical | CRITICAL | Must-not-forget knowledge |

### Sources

| Source | Code | Description |
|--------|------|-------------|
| Manual | MANUAL | Created by the user directly |
| Task | TASK | Captured from task lifecycle (events) |
| Goal | GOAL | Captured from goal lifecycle (events) |
| AI | AI | Generated or suggested by the AI Engine (future) |

## API Endpoints

Base path: `/api/v1/memories`

### Create Memory

`POST /api/v1/memories` (201 Created)

Request body:
```json
{
  "title": "User prefers morning workouts",
  "content": "The user consistently works out between 7am and 8am.",
  "type": "PREFERENCE",
  "importance": "HIGH",
  "source": "MANUAL",
  "sourceId": null,
  "tags": ["fitness", "routine"],
  "customMetadata": {}
}
```

Validation:
- `title` required, must not exceed 255 characters
- `content` required, must not exceed 10000 characters
- `tags` must not exceed 20 entries, each entry at most 50 characters
- `type`, `importance`, `source` default to NOTE, NORMAL, MANUAL when omitted

### List Memories

`GET /api/v1/memories` (200 OK)

Query parameters (all optional):
- `type` - filter by type
- `importance` - filter by importance
- `source` - filter by source
- `search` - keyword search across title and content (case-insensitive)
- `sortBy`, `sortOrder` - reserved for future sorting
- `page`, `limit` - reserved for future pagination

Response: array of memory objects, ordered by `created_at` DESC.

### Get Memory

`GET /api/v1/memories/{id}` (200 OK)

Returns a single memory. Returns 404 NOT_FOUND if the memory does not exist or does not belong to the authenticated user.

### Update Memory

`PATCH /api/v1/memories/{id}` (200 OK)

Partial update; any provided field is updated, omitted fields are left unchanged. Same validation constraints as Create. Returns 404 NOT_FOUND if not owned.

### Delete Memory

`DELETE /api/v1/memories/{id}` (200 OK)

Soft delete: sets `deleted_at`. Returns 404 NOT_FOUND if not owned.

## Business Rules

- **Strict user isolation**: every query is scoped by `user_id` and `deleted_at IS NULL`; a memory owned by another user is invisible and unreachable (404)
- **Soft delete only**: deleted memories are excluded from all reads; no restore endpoint in v1
- **Opaque source attribution**: `source_id` carries no foreign key and is not validated against other engines, keeping the Memory Engine decoupled
- **Trimming**: title, content, and tags are trimmed on create and update; blank tags are dropped
- **Defaults**: omitted `type`, `importance`, and `source` fall back to NOTE, NORMAL, MANUAL respectively
- **Custom metadata is engine-reserved**: `custom_metadata` JSONB is reserved for future AI payloads and should not be relied upon for core behavior in v1
- **Search priority**: when `search` is present it takes precedence over `type`/`importance`/`source` filters

## Events

The Memory Engine publishes the following domain events:

- `MemoryCreatedEvent` - fired when a new memory is created
- `MemoryUpdatedEvent` - fired when memory fields are modified
- `MemoryDeletedEvent` - fired on soft delete

Events are published within the transaction boundary and carry `memoryId` and `userId` for cross-engine consumers (e.g., future Notification and Analytics engines).

## Security

- All endpoints require authentication; the authenticated user is resolved server-side via `SecurityUtils.getCurrentUserId()`
- Controllers contain no business logic and only use the resolved user id
- Error responses use the standardized `ApiResponse` envelope with `success`, `data`, `error`, and `requestId`

## Error Handling

| Error Code | HTTP Status | Scenario |
|------------|-------------|----------|
| NOT_FOUND | 404 | Memory does not exist or is not owned by the user |
| VALIDATION_ERROR | 400 | Request body fails validation constraints |
| CONFLICT | 409 | Reserved for future conflicts |
| UNAUTHORIZED | 401 | Missing or invalid credentials |
| FORBIDDEN | 403 | User lacks permission |
| INTERNAL_ERROR | 500 | Unexpected server error |

## Future Roadmap

- Restore endpoint for soft-deleted memories
- Pagination and explicit sorting (filter contract already accepts `page`/`limit`/`sortBy`/`sortOrder`)
- Memory import/export (JSON)
- AI-generated memory suggestions and automatic signal extraction from task/goal events
- Memory relationships (linking related memories)
- Memory expiry / archival policies based on importance
- Full-text search (Postgres FTS / tsvector)
