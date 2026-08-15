# Memory Engine Design Document

## Version: 1.0.0
## Date: 2026-08-15
## Status: Accepted

---

## 1. Purpose

The Memory Engine owns the persistent knowledge records of a user inside THE SYSTEM. A memory is a durable, user-owned record that captures a fact, preference, note, insight, or system-generated signal about the user and their world.

The Memory Engine provides:
- Memory CRUD operations (create, read, update, delete)
- Memory classification (type, importance, source)
- Tagging and free-text content
- Strict per-user ownership and isolation
- A stable, source-attributed foundation for future engines (AI, Analytics) to consume
- Domain events for memory lifecycle changes

The Memory Engine is intentionally implemented as core CRUD first (Rule 41: Progressive Enhancement). AI interaction, automatic memory generation, and embedding/indexing are explicitly out of scope for v1.0.0.

## 2. Responsibilities

### Owned by Memory Engine

- Memory entities and lifecycle management
- Memory classification (NOTE, FACT, PREFERENCE, INSIGHT, SIGNAL)
- Memory importance levels (LOW, NORMAL, HIGH, CRITICAL)
- Memory source attribution (MANUAL, TASK, GOAL, AI)
- Memory tagging and free-text content
- Memory search and filtering
- Memory ownership and user isolation
- Memory soft deletion

### NOT Owned by Memory Engine

- User authentication (owned by Auth Engine)
- User profiles (owned by User Engine)
- Goals (owned by Goal Engine)
- Tasks and task execution (owned by Task Engine)
- XP, levels, achievements, streaks (owned by XP Engine)
- Settings (owned by Settings Engine)
- AI interactions and automatic memory generation (owned by AI Engine)
- Analytics computation (owned by Analytics Engine)
- Notifications (owned by Notification Engine)

## 3. Ownership and Boundaries

### 3.1 Engine Ownership

The Memory Engine exclusively owns the `memories` table and all memory-related business logic. No other engine may read or write memory records directly; cross-engine access happens only through service interfaces or domain events (Rule 32, Rule 34).

### 3.2 Cross-Engine Boundaries

- The Memory Engine publishes lifecycle events (`MemoryCreatedEvent`, `MemoryUpdatedEvent`, `MemoryDeletedEvent`).
- The AI Engine (future) may consume these events and read memories through a Memory service interface to build user context.
- The Analytics Engine (future) may consume these events for reporting.
- The Memory Engine does NOT consume events from other engines in v1.0.0. Automatic memory signals (e.g., from `TaskCompletedEvent`) are a documented future integration, not part of this phase.
- Memory rows reference other engines only by opaque UUID (`source_id`). No database foreign keys cross engine boundaries (Rule 34).

## 4. Database Design

### 4.1 memories Table

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK, DEFAULT gen_random_uuid() | Primary key |
| user_id | UUID | NOT NULL, FK to users(id) ON DELETE CASCADE | Owning user |
| title | VARCHAR(255) | NOT NULL | Short memory label |
| content | TEXT | NOT NULL | Memory body text |
| type | VARCHAR(20) | NOT NULL DEFAULT 'NOTE' | Memory classification |
| importance | VARCHAR(20) | NOT NULL DEFAULT 'NORMAL' | Relative importance |
| source | VARCHAR(20) | NOT NULL DEFAULT 'MANUAL' | Origin of the memory |
| source_id | UUID | NULL | UUID of the originating entity in another engine |
| tags | JSONB | NULL | Free-form string tags |
| custom_metadata | JSONB | NULL | Engine-specific extension space (future AI payloads) |
| created_at | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | Audit |
| updated_at | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | Audit |
| created_by | UUID | NULL | Audit |
| updated_by | UUID | NULL | Audit |
| deleted_at | TIMESTAMP | NULL | Soft delete |

### 4.2 Indexes

- `idx_memories_user_id` on memories(user_id)
- `idx_memories_user_type` on memories(user_id, type) WHERE deleted_at IS NULL
- `idx_memories_user_importance` on memories(user_id, importance) WHERE deleted_at IS NULL
- `idx_memories_user_source` on memories(user_id, source) WHERE deleted_at IS NULL
- `idx_memories_source_id` on memories(source_id) WHERE deleted_at IS NULL AND source_id IS NOT NULL
- `idx_memories_deleted_at` on memories(deleted_at)

### 4.3 Entity Relationships

- User 1:N memories (ON DELETE CASCADE)
- Memory N:1 source reference by opaque UUID only (no FK, cross-engine)

## 5. Memory Model

### 5.1 Field Justification

| Field | Justification |
|-------|---------------|
| user_id | Enforces per-user isolation (Rule 14, security constitution). |
| title | Short label for display, search, and future indexing. |
| content | The actual memory body. The primary payload. |
| type | Classification required for filtering and for the future AI Engine to treat memories differently. |
| importance | Lets users prioritize memories; supports future ranking. |
| source | Attribution required for the AI Engine to reason about where a memory came from. |
| source_id | Opaque cross-engine reference (e.g., the task that produced a signal). |
| tags | Lightweight free-form categorization, consistent with the Task Engine. |
| custom_metadata | Extension space for future AI payloads (embeddings, confidence, model version) without migrations. |

### 5.2 Memory Types

| Type | Code | Description |
|------|------|-------------|
| Note | NOTE | General-purpose user note. Default. |
| Fact | FACT | A discrete fact about the user or their world. |
| Preference | PREFERENCE | A stated user preference or context. |
| Insight | INSIGHT | A derived observation (future AI-generated). |
| Signal | SIGNAL | A system-generated memory from engine events (future). |

The five types are the smallest set that covers manual usage today (NOTE, FACT, PREFERENCE) and the documented future AI/event integrations (INSIGHT, SIGNAL) without speculative abstractions (Rule 30).

### 5.3 Importance Levels

| Level | Description |
|-------|-------------|
| LOW | Minor context. |
| NORMAL | Standard importance. Default. |
| HIGH | Important context worth surfacing. |
| CRITICAL | Foundational context. |

### 5.4 Memory Sources

| Source | Description |
|--------|-------------|
| MANUAL | Created directly by the user. Default. |
| TASK | Originates from task activity (future automatic capture). |
| GOAL | Originates from goal activity (future automatic capture). |
| AI | Generated or processed by the AI Engine (future). |

`source_id` is the opaque UUID of the originating entity when `source` is not MANUAL. It is always optional in v1.0.0.

## 6. CRUD Operations

| Operation | Method | Endpoint | Description |
|-----------|--------|----------|-------------|
| Create | POST | /api/v1/memories | Create a memory for the authenticated user. |
| List | GET | /api/v1/memories | List the user's memories with optional filters. |
| Get | GET | /api/v1/memories/{id} | Get a single memory owned by the user. |
| Update | PATCH | /api/v1/memories/{id} | Partially update a memory owned by the user. |
| Delete | DELETE | /api/v1/memories/{id} | Soft-delete a memory owned by the user. |

### 6.1 List Filters

- `type` — filter by MemoryType
- `importance` — filter by Importance
- `source` — filter by MemorySource
- `search` — case-insensitive keyword match on title or content
- `sortBy` / `sortOrder` — ordering (createdAt default, newest first)
- `page` / `limit` — accepted for future pagination (responses remain list-based, consistent with the Task Engine)

## 7. Validation Rules

### 7.1 Create

- `title`: required, 1-255 characters
- `content`: required, 1-10000 characters
- `type`: optional, valid MemoryType (default NOTE)
- `importance`: optional, valid Importance (default NORMAL)
- `source`: optional, valid MemorySource (default MANUAL)
- `sourceId`: optional, UUID
- `tags`: optional, at most 20 tags, each 1-50 characters
- `customMetadata`: optional, any JSON object

### 7.2 Update (PATCH semantics)

All fields optional. Provided fields are validated with the same rules as Create. `userId` is never accepted from the request body; it is always derived from the authenticated principal.

### 7.3 Business Rules

1. `userId` is always taken from the authenticated principal via `SecurityUtils.getCurrentUserId()`.
2. A user can only read, update, or delete their own memories. Accessing another user's memory returns NOT_FOUND (no existence leakage).
3. Soft delete is permanent from the user's perspective; no restore endpoint in v1.0.0 (consistent with not exposing restore until the Task Engine's restore pattern is required by memory use cases).
4. `tags` are normalized to trimmed, non-blank strings.

## 8. API Contract

Base path: `/api/v1/memories`

### 8.1 Success Responses

- `POST /api/v1/memories` → 201 Created, `MemoryResponse`
- `GET /api/v1/memories` → 200 OK, `List<MemoryResponse>`
- `GET /api/v1/memories/{id}` → 200 OK, `MemoryResponse`
- `PATCH /api/v1/memories/{id}` → 200 OK, `MemoryResponse`
- `DELETE /api/v1/memories/{id}` → 200 OK, no data

All responses use the standard `ApiResponse` envelope (Rule 13).

### 8.2 Error Responses

| Code | HTTP Status | Condition |
|------|-------------|-----------|
| NOT_FOUND | 404 | Memory not found or not owned by the user |
| VALIDATION_ERROR | 400 | Bean validation failure |
| INTERNAL_ERROR | 500 | Unexpected failure |

## 9. Domain Events

The Memory Engine publishes the following domain events (all implement `DomainEvent`):

- `MemoryCreatedEvent(memoryId, userId, type, title, occurredAt)` — fired when a memory is created
- `MemoryUpdatedEvent(memoryId, userId, type, title, occurredAt)` — fired when a memory is updated
- `MemoryDeletedEvent(memoryId, userId, occurredAt)` — fired on soft delete

Events use UUID identifiers, consistent with `UserProfileCreatedEvent` and `TaskCompletedEvent`. Events are published within the transaction after persistence (Rule 29, Rule 20).

### Future Consumers

- AI Engine: consume memory events to maintain user context; generate INSIGHT/SIGNAL memories.
- Analytics Engine: consume memory events for usage reporting.

## 10. Security

- All `/api/v1/memories/**` endpoints require authentication (SecurityConfig default deny).
- Ownership is enforced in the service layer: every repository query is scoped by `userId` AND `deletedAt IS NULL`.
- No public endpoints. No admin-only endpoints.
- `userId` is never trusted from the request body.

## 11. Error Handling

The module provides `MemoryExceptionHandler` (`@RestControllerAdvice` scoped to the memory package) that maps:
- `BusinessException(NOT_FOUND)` → 404
- `BusinessException(CONFLICT)` → 409 (reserved)
- `BusinessException(UNAUTHORIZED)` → 401 (reserved)
- `BusinessException(FORBIDDEN)` → 403 (reserved)
- other `BusinessException` → 400
- `MethodArgumentNotValidException` → 400 VALIDATION_ERROR
- unexpected exceptions → 500 INTERNAL_ERROR (logged)

This mirrors the corrected `UserExceptionHandler` pattern (per api-constitution and the audit).

## 12. Future Compatibility with the AI Engine

The following foundations are included in v1.0.0 without implementing AI:

1. `type` supports INSIGHT and SIGNAL so AI-generated and event-generated memories have a classification.
2. `source` and `source_id` attribute memories to their origin (AI, TASK, GOAL).
3. `custom_metadata` JSONB provides an extension space for future AI payloads (embeddings, confidence, model version) without schema migration.
4. Lifecycle events give the AI Engine a change stream to build and refresh user context.
5. The Memory service interface is the single entry point the AI Engine will use to query memory records.

The AI Engine itself is out of scope for this phase (Rule 41).

## 13. Testing Strategy

- **Unit tests (service)**: create/get/list/update/delete flows, not-found behavior, user-isolation (a memory owned by another user is not retrievable/updatable/deletable), validation of null/blank values, event publishing (capture published events).
- **Controller integration tests**: all 5 endpoints with mocked service, status codes, request/response envelope, bean validation failures, not-found mapping.
- **Repository integration test**: H2-backed persistence, soft-delete filtering, user-scoped queries.

## 14. Future Roadmap

- Automatic memory capture from Task/Goal completion events (SIGNAL type)
- Memory pinning/favorites
- Full-text search and pagination
- Memory sharing (public memory links)
- AI-assisted memory summarization and insights
- Embedding/indexing for semantic recall
- Memory import/export

---

## Appendix A: Glossary

| Term | Definition |
|------|------------|
| Memory | A durable, user-owned knowledge record in THE SYSTEM |
| Memory Type | Classification of a memory (NOTE, FACT, PREFERENCE, INSIGHT, SIGNAL) |
| Importance | User-assigned relevance level (LOW, NORMAL, HIGH, CRITICAL) |
| Source | Origin of a memory (MANUAL, TASK, GOAL, AI) |
| Soft Delete | Marking a record as deleted without removing it from the database |

## Appendix B: References

- Backend Constitution (Rules 3, 13, 14, 15, 20, 26, 27, 29-35, 41, 43)
- Roadmap (Phase 7: Memory module)
- ADR-0001: Modular Monolith Architecture
- ADR-0005: Kernel Evolution Philosophy
- ADR-0007: Task Execution Provider Pattern (task completion signals)
- Task Engine Design Document (integration points)
- Goal Engine Design Document
