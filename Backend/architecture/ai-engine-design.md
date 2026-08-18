# AI Engine Design Document

## Version: 1.0.0
## Date: 2026-08-15
## Status: Accepted

---

## 1. Purpose

The AI Engine owns AI interactions inside THE SYSTEM. An AI interaction is a single, user-initiated request that is processed against an AI provider and recorded as durable per-user history.

The AI Engine provides:
- Provider-agnostic AI interaction processing (create, list, get, delete)
- A minimal `AiProvider` abstraction that isolates the engine from concrete LLM providers
- Deterministic construction of user context from the Memory Engine's service interface
- Persistent, user-owned interaction history
- A domain event for interaction lifecycle changes
- Explicit, safe behavior when no AI provider is configured

The AI Engine is intentionally implemented as core interactions first (Rule 41: Progressive Enhancement). Concrete LLM providers, automatic memory generation, embedding/indexing, and AI-driven suggestions for other engines are explicitly out of scope for v1.0.0.

## 2. Responsibilities

### Owned by AI Engine

- AI interaction entities and lifecycle management
- The `AiProvider` abstraction and provider registry
- Provider selection via application configuration
- Deterministic AI context construction from user memories
- Interaction history persistence and user isolation
- Interaction soft deletion (privacy purge)
- `AiInteractionCreatedEvent`

### NOT Owned by AI Engine

- User authentication (owned by Auth Engine)
- User profiles (owned by User Engine)
- Goals (owned by Goal Engine)
- Tasks and task execution (owned by Task Engine)
- XP, levels, achievements, streaks (owned by XP Engine)
- Settings (owned by Settings Engine)
- Memory entities and memory lifecycle (owned by Memory Engine; AI reads through the Memory service interface only)
- Automatic memory generation (INSIGHT/SIGNAL memories) — documented future integration
- Goal/Task/XP suggestions — documented future integration (owned by those engines when integrated)
- Embeddings, vector search, and semantic recall — explicitly deferred
- Concrete LLM provider integrations — explicitly deferred (see Section 5)

## 3. Ownership and Boundaries

### 3.1 Engine Ownership

The AI Engine exclusively owns the `ai_interactions` table and all interaction business logic. No other engine may read or write interaction records directly; cross-engine access happens only through service interfaces or domain events (Rule 32, Rule 34).

### 3.2 Cross-Engine Boundaries

- The AI Engine reads memories through the `MemoryService` interface (the documented single entry point for the AI Engine per the Memory Engine design, Section 12.5). It never accesses `MemoryRepository` directly.
- The AI Engine publishes `AiInteractionCreatedEvent`; future engines (e.g., Analytics) may consume it.
- The AI Engine does NOT consume events from other engines in v1.0.0. Consuming `MemoryCreatedEvent`/`MemoryUpdatedEvent`/`MemoryDeletedEvent` to maintain a context cache is a documented future integration, not part of this phase; v1 builds context on demand.
- Interaction rows reference the owning user only by `user_id` foreign key. No database foreign keys point into other engines.

## 4. Database Design

### 4.1 ai_interactions Table

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | UUID | PK, DEFAULT gen_random_uuid() | Primary key |
| user_id | UUID | NOT NULL, FK to users(id) ON DELETE CASCADE | Owning user |
| message | TEXT | NOT NULL | The user's request text |
| response | TEXT | NOT NULL | The provider's response text |
| provider | VARCHAR(50) | NOT NULL | Name of the provider that served the interaction |
| model | VARCHAR(100) | NULL | Provider model identifier (when the provider reports one) |
| context | JSONB | NULL | Deterministic context items sent to the provider (audit/reproducibility) |
| prompt_tokens | INTEGER | NULL | Provider-reported prompt token usage |
| completion_tokens | INTEGER | NULL | Provider-reported completion token usage |
| total_tokens | INTEGER | NULL | Provider-reported total token usage |
| finish_reason | VARCHAR(50) | NULL | Provider-reported completion reason |
| created_at | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | Audit |
| updated_at | TIMESTAMP | NOT NULL DEFAULT CURRENT_TIMESTAMP | Audit |
| created_by | UUID | NULL | Audit |
| updated_by | UUID | NULL | Audit |
| deleted_at | TIMESTAMP | NULL | Soft delete |

Only successful interactions are persisted in v1.0.0: a failed provider call produces no interaction record and no side effects. A `status` column is intentionally omitted (Rule 30) because every persisted row is, by construction, a completed interaction.

### 4.2 Indexes

- `idx_ai_interactions_user_id` on ai_interactions(user_id)
- `idx_ai_interactions_user_created` on ai_interactions(user_id, created_at) WHERE deleted_at IS NULL (history listing)
- `idx_ai_interactions_deleted_at` on ai_interactions(deleted_at)

### 4.3 Entity Relationships

- User 1:N ai_interactions (ON DELETE CASCADE)
- No cross-engine FKs (Rule 34)

## 5. Provider Architecture

### 5.1 Abstraction

The AI Engine is decoupled from concrete LLM providers behind the `AiProvider` interface:

```java
public interface AiProvider {
    String name();
    boolean isConfigured();
    AiProviderResponse generate(AiProviderRequest request);
}
```

- `name()` — stable provider identifier used for configuration.
- `isConfigured()` — whether the provider is registered AND able to serve requests (e.g., a provider requiring a key reports `false` until configured).
- `generate(...)` — performs the AI request. Providers signal failures by throwing `AiProviderException`.

Request/response payloads are records:

```java
public record AiContextItem(String title, String type, String importance, String source, String content) {}
public record AiProviderRequest(String message, List<AiContextItem> context) {}
public record AiProviderResponse(String content, String model,
        Integer promptTokens, Integer completionTokens, Integer totalTokens, String finishReason) {}
```

`AiProviderRegistry` collects every `AiProvider` bean at startup and resolves providers by name. The AI service, controllers, context builder, and persistence logic depend only on the abstraction, so future providers can be added without rewriting the engine.

### 5.2 Configured Providers

**v1.0.0 ships with zero concrete providers.** The repository documentation specifies no LLM provider, no API-key mechanism, and no data-egress rules (per design review). Implementing a production provider integration is therefore out of scope and would violate Rule 30 (Engineering Over Speed). The abstraction, registry, selection logic, and failure semantics are implemented and fully tested; a concrete provider is registered by adding a single `AiProvider` bean.

### 5.3 Configuration Source

Provider selection comes from the established application configuration mechanism (environment variables, matching the `thesystem.jwt.*` pattern):

```
thesystem.ai.provider=${THE_SYSTEM_AI_PROVIDER:}
```

No credentials, keys, or tokens exist in v1.0.0 source code (Rule 36). When a concrete provider requiring secrets is added later, the secret must arrive via environment variable / secret management and never be committed.

### 5.4 Failure Behavior

| Scenario | Behavior |
|----------|----------|
| No provider name configured | 503 SERVICE_UNAVAILABLE, no interaction persisted |
| Configured name has no registered provider | 503 SERVICE_UNAVAILABLE, no interaction persisted |
| Provider reports `isConfigured() == false` | 503 SERVICE_UNAVAILABLE, no interaction persisted |
| Provider throws `AiProviderException` | 503 SERVICE_UNAVAILABLE (logged as warning with provider name), no interaction persisted |
| Provider throws any other runtime exception | 503 SERVICE_UNAVAILABLE (logged as error), no interaction persisted |

Failed interactions are never persisted and never emit events (no side effects after failed operations).

### 5.5 Test Strategy

Providers are exercised with mocks/fakes in unit tests. Normal automated tests never require a real external provider or API key. Registry behavior, provider failure, and no-provider behavior are covered by unit tests; no concrete provider is present in tests.

## 6. Memory Integration

The AI Engine consumes Memory exactly as documented by the Memory Engine design: through the `MemoryService` interface, which is the single entry point for the AI Engine to query memory records.

### 6.1 Context Construction

When `includeMemoryContext` is true (default), the AI Engine builds context deterministically:

1. Query `MemoryService.listMemories(userId, emptyFilter)` — returns the user's non-deleted memories ordered by `createdAt` DESC (the Memory Engine's documented default ordering).
2. Map each memory to an `AiContextItem(title, type, importance, source, content)`.
3. Cap the context at the most recent 50 items (configurable constant) so memories are never dumped wholesale into a prompt.

This is intentionally deterministic and bounded. No embeddings, vector search, or relevance ranking are introduced (Rule 30). Importance-based ranking and semantic recall are documented future work.

The resulting `List<AiContextItem>` is passed to the provider and persisted as `context` (JSONB) for audit and reproducibility. Users can opt out per request via `includeMemoryContext = false`.

### 6.2 What AI Does NOT Do with Memory

- AI does not create, update, or delete memories in v1.0.0. Automatic generation of INSIGHT/SIGNAL memories is a documented future capability.
- AI does not bypass `MemoryService`; it never touches `MemoryRepository` or the `memories` table.
- No second memory store exists inside AI.

## 7. Security

- Every `/api/v1/ai/**` endpoint requires authentication (SecurityConfig default deny; no security configuration change needed).
- The authenticated user is resolved server-side via `SecurityUtils.getCurrentUserId()`; `userId` is never accepted from the request body.
- All repository queries are scoped by `userId` AND `deletedAt IS NULL`. Accessing another user's interaction returns NOT_FOUND (no existence leakage).
- Sensitive content is never logged: interaction message/response text is excluded from all log statements. Only IDs, provider name, and token counts are logged (Rule 16).
- Provider credentials are protected by design: there are none in v1.0.0, and future secrets must come from environment variables.
- Context sent to a provider contains only the authenticated user's own memories, and only when the user has not opted out.

## 8. API Contract

Base path: `/api/v1/ai`

### 8.1 Endpoints

| Operation | Method | Endpoint | Description |
|-----------|--------|----------|-------------|
| Create | POST | /api/v1/ai/interactions | Run an AI interaction for the authenticated user. |
| List | GET | /api/v1/ai/interactions | List the user's AI interactions (newest first). |
| Get | GET | /api/v1/ai/interactions/{id} | Get a single interaction owned by the user. |
| Delete | DELETE | /api/v1/ai/interactions/{id} | Soft-delete an interaction owned by the user (privacy purge). |

There is no update endpoint: an interaction is an immutable record of a request/response pair. Creating a new interaction with a revised message is the only supported flow.

### 8.2 Request DTO — CreateAiInteractionRequest

- `message`: required, 1-4000 characters
- `includeMemoryContext`: optional boolean, default true

### 8.3 Response DTO — AiInteractionResponse

`id`, `userId`, `message`, `response`, `provider`, `model`, `context` (List<AiContextItem>), `promptTokens`, `completionTokens`, `totalTokens`, `finishReason`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `deletedAt`.

### 8.4 Status Codes

- `POST /api/v1/ai/interactions` → 201 Created, `AiInteractionResponse`
- `GET /api/v1/ai/interactions` → 200 OK, `List<AiInteractionResponse>`
- `GET /api/v1/ai/interactions/{id}` → 200 OK, `AiInteractionResponse`
- `DELETE /api/v1/ai/interactions/{id}` → 200 OK, no data
- All responses use the standard `ApiResponse` envelope (Rule 13).

### 8.5 Error Responses

| Code | HTTP Status | Condition |
|------|-------------|-----------|
| SERVICE_UNAVAILABLE | 503 | No provider configured, provider unavailable, or provider failure |
| NOT_FOUND | 404 | Interaction not found or not owned by the user |
| VALIDATION_ERROR | 400 | Bean validation failure |
| INTERNAL_ERROR | 500 | Unexpected failure |

## 9. Validation Rules

### 9.1 Create

- `message`: required, 1-4000 characters (trimmed on persist)
- `includeMemoryContext`: optional, boolean

### 9.2 Business Rules

1. `userId` is always derived from the authenticated principal via `SecurityUtils.getCurrentUserId()`.
2. A user can only read or delete their own interactions. Accessing another user's interaction returns NOT_FOUND.
3. Only successful interactions are persisted. Provider failures produce no record and no event.
4. Context is bounded to the most recent 50 memories when memory context is requested.
5. Soft delete is permanent from the user's perspective; no restore endpoint in v1.0.0.
6. Interactions are immutable: no update endpoint.

## 10. Domain Events

The AI Engine publishes the following domain event (implements `DomainEvent`):

- `AiInteractionCreatedEvent(interactionId, userId, provider, occurredAt)` — fired after a successful interaction is persisted.

Events use UUID identifiers and are published within the transaction after persistence (Rule 29, Rule 20), consistent with `MemoryCreatedEvent`.

### Future Consumers

- Analytics Engine: interaction usage reporting.
- Notification Engine: completion alerts.

## 11. Error Handling

The module provides `AiExceptionHandler` (`@RestControllerAdvice` scoped to the `com.thesystem.modules.ai` package) that maps:
- `BusinessException(SERVICE_UNAVAILABLE)` → 503
- `BusinessException(NOT_FOUND)` → 404
- `BusinessException(CONFLICT)` → 409 (reserved)
- `BusinessException(UNAUTHORIZED)` → 401 (reserved)
- `BusinessException(FORBIDDEN)` → 403 (reserved)
- other `BusinessException` → 400
- `MethodArgumentNotValidException` → 400 VALIDATION_ERROR
- unexpected exceptions → 500 INTERNAL_ERROR (logged)

This mirrors the `MemoryExceptionHandler` pattern.

## 12. Logging

- Log interaction creation with `id`, `userId`, `provider`, and token counts.
- Never log `message`, `response`, or `context` content (Rule 16).
- Provider failures are logged with provider name and error message (no request payload).

## 13. Configuration

| Property | Environment Variable | Default | Description |
|----------|----------------------|---------|-------------|
| `thesystem.ai.provider` | `THE_SYSTEM_AI_PROVIDER` | (empty) | Name of the active AI provider |

Registered via `application.yml`. No settings registry entry is required in v1.0.0; the Settings Engine `ai` namespace is a documented future integration.

## 14. Frontend Integration

The AI Engine exposes an interaction console:
- Composer to send a message (with memory-context toggle)
- Response display with provider/model/usage metadata
- Interaction history list with delete (privacy purge)
- Route `/ai`, sidebar entry, `aiApi.js` client, `AiPage.jsx`, `ai.css`

The interface intentionally implements the interaction endpoint and history only; no autonomous chat agent, streaming, or multi-turn conversation model is built in v1.0.0.

## 15. Testing Strategy

- **Service unit tests**: success flow (persist + event), context construction and bounding, `includeMemoryContext=false` (no memory query), no-provider → 503, unknown provider → 503, provider `isConfigured()==false` → 503, provider failure → 503 with no persistence and no event, not-found behavior, user isolation, list, delete.
- **Controller integration tests**: all 4 endpoints with mocked service, status codes, request/response envelope, bean validation failures, not-found mapping.
- **Repository integration test**: H2-backed persistence, id generation, user-scoped queries, soft-delete filtering.
- **Provider/registry unit tests**: registry resolution by name; provider failure contract via a fake provider.
- No test requires a real external provider or API key.

## 16. Future Roadmap

- Concrete providers behind the existing abstraction (local-first: Ollama; later: hosted providers) with secrets via environment variables
- Automatic generation of INSIGHT memories from interaction outcomes
- Consuming Memory lifecycle events to maintain a context cache
- Context selection by importance ranking and (later) embeddings/semantic recall
- Goal/Task/XP suggestions (owned by AI Engine, integrated with those engines)
- Streaming responses and multi-turn conversations
- Interaction status tracking (PENDING/FAILED) with failed-interaction persistence
- Settings Engine `ai` namespace registration

---

## Appendix A: Glossary

| Term | Definition |
|------|------------|
| AI Interaction | A single user-initiated request processed against an AI provider and recorded as history |
| Provider | A backend implementing the `AiProvider` abstraction |
| Context | Deterministic, bounded memory-derived context passed to a provider |
| Soft Delete | Marking a record as deleted without removing it from the database |

## Appendix B: References

- Backend Constitution (Rules 3, 13, 14, 15, 16, 20, 26, 29-36, 41, 43)
- Roadmap (Phase 7: AI module)
- ADR-0001: Modular Monolith Architecture
- ADR-0005: Kernel Evolution Philosophy
- Memory Engine Design Document (Sections 5, 12: service interface as the AI Engine's entry point)
- Settings Engine Design Document (ai namespace as future integration)
- Module Constitution (package structure, module README)
