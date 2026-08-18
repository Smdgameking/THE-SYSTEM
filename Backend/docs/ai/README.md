# AI Engine

## Purpose

The AI Engine owns AI interactions inside THE SYSTEM. An AI interaction is a single, user-initiated request processed against an AI provider and recorded as durable, user-owned history. The engine is deliberately provider-agnostic: it defines the `AiProvider` abstraction but ships with no concrete provider in v1.0.0.

The AI Engine provides:
- Provider-agnostic AI interaction processing (create, list, get, delete)
- A minimal `AiProvider` abstraction and provider registry
- Deterministic construction of user context from the Memory Engine's service interface
- Persistent, user-scoped interaction history with soft delete (privacy purge)
- Explicit, safe behavior when no provider is configured (503 SERVICE_UNAVAILABLE)
- Domain event publishing for interaction lifecycle changes

## Responsibilities

### Owned by AI Engine

- AI interaction entities and lifecycle management
- `AiProvider` abstraction and provider registry
- Provider selection via application configuration
- Deterministic AI context construction from user memories
- Interaction history persistence and user isolation
- Interaction soft deletion (privacy purge)
- `AiInteractionCreatedEvent`

### NOT Owned by AI Engine

- User authentication (Auth Engine)
- User profiles (User Engine)
- Goals (Goal Engine)
- Tasks and task execution (Task Engine)
- XP, levels, achievements, streaks (XP Engine)
- Settings (Settings Engine)
- Memory entities and lifecycle (Memory Engine; AI reads through the Memory service interface only)
- Automatic memory generation (INSIGHT/SIGNAL memories) - future
- Goal/Task/XP suggestions - future
- Embeddings, vector search, and semantic recall - future
- Concrete LLM provider integrations - future

## Architecture

Package: `com.thesystem.modules.ai`

```
controller/   REST controller for interactions
dto/          CreateAiInteractionRequest, AiInteractionResponse
entity/       AiInteraction
events/       AiInteractionCreatedEvent
exception/    AiExceptionHandler
mapper/       AiInteractionMapper (MapStruct)
provider/     AiProvider abstraction, registry, request/response records, AiProviderException
repository/   AiInteractionRepository
service/      AiService interface and AiServiceImpl
```

The AI Engine depends on the Memory Engine only through the `MemoryService` interface (the documented entry point for the AI Engine to query memory records). No direct repository access, no second memory store, no circular dependency.

### Interaction Flow

```
User request
     ↓
AiController (userId from authenticated principal)
     ↓
AiServiceImpl
     ↓
resolve provider (config -> registry)
     ↓
build context (MemoryService.listMemories -> bounded AiContextItem list)
     ↓
AiProvider.generate(request)
     ↓
persist AiInteraction + publish AiInteractionCreatedEvent
     ↓
AiInteractionResponse
```

Failed interactions are never persisted and never emit events (no side effects after failed operations).

## Database

### ai_interactions table

- `id` UUID PRIMARY KEY (gen_random_uuid)
- `user_id` UUID NOT NULL (FK to users, ON DELETE CASCADE)
- `message` TEXT NOT NULL
- `response` TEXT NOT NULL
- `provider` VARCHAR(50) NOT NULL
- `model` VARCHAR(100) NULL
- `context` JSONB NULL (bounded context items sent to the provider)
- `prompt_tokens` INTEGER NULL
- `completion_tokens` INTEGER NULL
- `total_tokens` INTEGER NULL
- `finish_reason` VARCHAR(50) NULL
- `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- `created_by` UUID NULL
- `updated_by` UUID NULL
- `deleted_at` TIMESTAMP NULL

Only successful interactions are persisted. A `status` column is intentionally omitted because every persisted row is, by construction, a completed interaction.

### Indexes

- `idx_ai_interactions_user_id` on ai_interactions(user_id)
- `idx_ai_interactions_user_created` on ai_interactions(user_id, created_at) WHERE deleted_at IS NULL
- `idx_ai_interactions_deleted_at` on ai_interactions(deleted_at)

### Relationships

- User 1:N ai_interactions (ON DELETE CASCADE)
- No cross-engine foreign keys

## Provider Architecture

The engine is decoupled from concrete LLM providers behind the `AiProvider` interface:

```java
public interface AiProvider {
    String name();
    boolean isConfigured();
    AiProviderResponse generate(AiProviderRequest request);
}
```

- `name()` - stable provider identifier used for configuration
- `isConfigured()` - whether the provider is registered AND able to serve requests
- `generate(...)` - performs the AI request; failures are signaled by throwing `AiProviderException`

`AiProviderRegistry` collects every `AiProvider` bean at startup and resolves providers by name. Future providers are added by registering a single bean; the service, controller, context builder, and persistence logic are unchanged.

### Configured Providers

**v1.0.0 ships with zero concrete providers.** No LLM provider, API-key mechanism, or data-egress rules are documented in the repository, so a production provider integration is out of scope. The abstraction, registry, selection logic, and failure semantics are implemented and fully tested.

### Configuration Source

Provider selection comes from the established application configuration mechanism (environment variables):

| Property | Environment Variable | Default |
|----------|----------------------|---------|
| `thesystem.ai.provider` | `THE_SYSTEM_AI_PROVIDER` | (empty) |

No credentials, keys, or tokens exist in v1.0.0 source code. Future secrets must arrive via environment variables / secret management and never be committed.

### Failure Behavior

| Scenario | Behavior |
|----------|----------|
| No provider name configured | 503 SERVICE_UNAVAILABLE, nothing persisted |
| Configured name has no registered provider | 503 SERVICE_UNAVAILABLE, nothing persisted |
| Provider reports `isConfigured() == false` | 503 SERVICE_UNAVAILABLE, nothing persisted |
| Provider throws `AiProviderException` | 503 SERVICE_UNAVAILABLE, nothing persisted |
| Provider throws any other runtime exception | 503 SERVICE_UNAVAILABLE, nothing persisted |

## API Endpoints

Base path: `/api/v1/ai`

### Create AI Interaction

`POST /api/v1/ai/interactions` (201 Created)

Request body:
```json
{
  "message": "Summarize what I should focus on today.",
  "includeMemoryContext": true
}
```

Validation:
- `message` required, at most 4000 characters
- `includeMemoryContext` optional boolean, default true

Response: `AiInteractionResponse` (id, userId, message, response, provider, model, context, promptTokens, completionTokens, totalTokens, finishReason, timestamps).

### List AI Interactions

`GET /api/v1/ai/interactions` (200 OK)

Returns the user's interactions ordered by `created_at` DESC.

### Get AI Interaction

`GET /api/v1/ai/interactions/{id}` (200 OK)

Returns a single interaction. Returns 404 NOT_FOUND if the interaction does not exist or does not belong to the authenticated user.

### Delete AI Interaction

`DELETE /api/v1/ai/interactions/{id}` (200 OK)

Soft delete: sets `deleted_at` (privacy purge). Returns 404 NOT_FOUND if not owned. There is no update endpoint: interactions are immutable records.

## Business Rules

- **Strict user isolation**: every query is scoped by `user_id` and `deleted_at IS NULL`; an interaction owned by another user is invisible and unreachable (404)
- **Only successful interactions persist**: provider failures produce no record and no event
- **Immutable interactions**: no update endpoint; a revised message is a new interaction
- **Bounded context**: at most the most recent 50 memories are passed as context when memory context is enabled
- **Context opt-out**: `includeMemoryContext: false` skips memory queries entirely
- **Soft delete only**: deleted interactions are excluded from all reads; no restore endpoint in v1
- **No content logging**: message/response/context text is never logged; only IDs, provider, and token counts are

## Memory Integration

The AI Engine consumes Memory exactly as documented by the Memory Engine design: through the `MemoryService` interface (the single entry point for the AI Engine to query memory records). It never touches `MemoryRepository` or the `memories` table directly.

Context construction is deterministic: the user's non-deleted memories are fetched via `MemoryService.listMemories(userId, emptyFilter)` (ordered `createdAt` DESC by the Memory Engine), mapped to `AiContextItem(title, type, importance, source, content)`, and bounded to the most recent 50. The context is passed to the provider and persisted as `context` (JSONB) for audit and reproducibility. No embeddings, vector search, or relevance ranking are used in v1.

The AI Engine does NOT create, update, or delete memories in v1. Automatic generation of INSIGHT/SIGNAL memories is a documented future capability.

## Security

- All `/api/v1/ai/**` endpoints require authentication (SecurityConfig default deny)
- The authenticated user is resolved server-side via `SecurityUtils.getCurrentUserId()`; `userId` is never accepted from the request body
- Cross-user access returns NOT_FOUND (no existence leakage)
- Sensitive content is never logged (message/response/context)
- No provider credentials exist in v1; future secrets come from environment variables
- Context sent to a provider contains only the authenticated user's own memories, and only when the user has not opted out

## Error Handling

| Error Code | HTTP Status | Scenario |
|------------|-------------|----------|
| SERVICE_UNAVAILABLE | 503 | No provider configured, provider unavailable, or provider failure |
| NOT_FOUND | 404 | Interaction does not exist or is not owned by the user |
| VALIDATION_ERROR | 400 | Request body fails validation constraints |
| CONFLICT | 409 | Reserved for future conflicts |
| UNAUTHORIZED | 401 | Missing or invalid credentials |
| FORBIDDEN | 403 | User lacks permission |
| INTERNAL_ERROR | 500 | Unexpected server error |

## Events

The AI Engine publishes:

- `AiInteractionCreatedEvent(interactionId, userId, provider, occurredAt)` - fired after a successful interaction is persisted

Events use UUID identifiers and are published within the transaction after persistence. No events are consumed in v1.0.0; consuming Memory lifecycle events to maintain a context cache is documented future work.

## Testing

- **Service unit tests**: success flow (persist + event), context construction and bounding, context opt-out, no-provider / unknown-provider / unconfigured-provider → 503, provider failure → 503 with no persistence and no event, not-found behavior, user isolation, list, delete
- **Controller integration tests**: all 4 endpoints, response envelope, bean validation failures, 404 mapping, 503 mapping
- **Repository integration tests**: H2-backed persistence, id generation, user scoping, soft-delete filtering
- **Registry unit tests**: provider resolution by name, empty/unknown/null lookups
- No test requires a real external provider or API key

## Future Roadmap

- Concrete providers behind the existing abstraction (local-first: Ollama; later: hosted providers) with secrets via environment variables
- Automatic generation of INSIGHT memories from interaction outcomes
- Consuming Memory lifecycle events to maintain a context cache
- Context selection by importance ranking and (later) embeddings/semantic recall
- Goal/Task/XP suggestions (owned by AI Engine, integrated with those engines)
- Streaming responses and multi-turn conversations
- Interaction status tracking (PENDING/FAILED) with failed-interaction persistence
- Settings Engine `ai` namespace registration

## References

- Design: `architecture/ai-engine-design.md`
- Migration: `db/migration/V21__create_ai_interactions_table.sql`
- Memory Engine: `architecture/memory-engine-design.md`, `docs/memory/README.md`
- Backend Constitution: Rules 3, 13, 14, 15, 16, 20, 26, 29-36, 41, 43
- ADR-0001: Modular Monolith Architecture
- ADR-0005: Kernel Evolution Philosophy
