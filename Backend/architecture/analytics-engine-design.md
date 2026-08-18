# Analytics Engine Design

## Version
1.0.0

## Date
2026-08-15

## Status
Accepted

---

> The Analytics Engine is a **reporting engine**. Every reported metric is derived on-demand from persisted data owned by the Task, Goal, XP, Streak, Memory, and AI engines, accessed **exclusively through their service interfaces**. It owns **no tables** and **duplicates no data** (Rule 31). No metric is reported unless it can be traced to a persisted column. This document records exactly which metrics are derivable, which were rejected, and how the engine is built.

---

## 1. Purpose

Implement roadmap line 101 — `[ ] Analytics module (reporting)` (Phase 8: Supporting Modules, v0.9.0 – v1.0.0). The Analytics Engine provides per-user reporting across engines:

- **Time-series trends** (daily XP earned, tasks completed, focus minutes, activity days, goals completed) over a bounded look-back window.
- **Usage reporting** for the Memory Engine ("Analytics Engine: consume memory events for usage reporting" — memory-engine-design §9) and the AI Engine ("Analytics Engine: interaction usage reporting" — ai-engine-design §10).
- **A cross-domain overview** snapshot composing existing per-engine statistics (Task, Goal, XP, Streak) plus the derived Memory/AI usage summaries.

The engine is read-only: it introduces no new domain events, no new tables, and no writes to any engine's data.

## 2. Derivable Metrics — Derivation Analysis

This is the analysis requested for the milestone: **exactly which analytics data can be derived from existing persisted data**, and which were deliberately rejected.

### 2.1 Already computed by owning engines (composed, never re-derived)

| Metric | Persisted source | Owning engine | Existing access |
| --- | --- | --- | --- |
| Current XP, level, lifetime XP | `xp_accounts` (V9) | XP | `XpService.getAccount(userId)` |
| Daily / weekly / monthly / lifetime XP, level, tasks/goals completed, achievements unlocked | `xp_transactions`, `xp_accounts`, `user_achievements` (V9) | XP | `XpService.getStatistics()` |
| Current / longest streak | `user_streaks` (V17) | XP (Streak) | `XpService.getUserStreak(userId)` |
| Task completion rate, totals, overdue, focus today/week, by-status/priority/category | `tasks`, `task_time_entries` (V8) | Task | `TaskService.getStatistics(userId)` |
| Goal totals, active/completed, avg completion %, by-status/priority | `goals` (V7) | Goal | `GoalService.getStatistics(userId)` |

These are **composed** into the Analytics overview — not recomputed. Recomputing them would duplicate business logic owned by other engines (Rule 32).

### 2.2 Derivable but not yet exposed (new — added via owning-engine service interfaces)

| # | Metric | Persisted source (table.column) | Owning engine | Access path |
| --- | --- | --- | --- | --- |
| 1 | Daily XP earned series | `xp_transactions.created_at` + `amount > 0` (V9) | XP | `XpService.getTransactionHistory(userId, fromDate, toDate)` (existing) |
| 2 | Tasks completed per day series | `tasks.completed_date` (V8) | Task | `TaskService.listTasks(userId, filter)` (existing) |
| 3 | Goals completed per day series | `goals.completed_date` (V7) | Goal | `GoalService.getGoals(userId, filter)` (existing) |
| 4 | Focus minutes per day series | `task_time_entries.start_time` + `duration_minutes` (V8) | Task | **NEW** `TaskService.listTimeEntriesForPeriod(userId, from, to)` |
| 5 | Activity days series | `user_streak_history.activity_date` (V17) | XP (Streak) | **NEW** `XpService.getActivityTrend(userId, from, to)` |
| 6 | XP earned by day of week | `xp_transactions.created_at` + `amount > 0` (V9) | XP | derived from #1 window (matches xp-engine-design §13.3 `by_time`) |
| 7 | AI interaction usage (count, tokens, by provider/model, per-day) | `ai_interactions.created_at`, `provider`, `model`, `prompt_tokens`, `completion_tokens`, `total_tokens` (V21) | AI | `AiService.listInteractions(userId)` (existing) |
| 8 | Memory usage (count, by type/importance/source, per-month) | `memories.type`, `importance`, `source`, `created_at` (V20) | Memory | `MemoryService.listMemories(userId, filter)` (existing) |

The two **NEW** methods are the only cross-engine surface changes of this milestone. Both are read-only, per-user, and implemented inside their owning engines (they query the owning engine's own repository — Rule 34).

### 2.3 Explicitly rejected (NOT derivable → NOT reported)

| Candidate metric | Reason rejected |
| --- | --- |
| Habit / sleep / health / calendar / integration analytics | Engines not implemented; no persisted tables exist. Inventing them would contradict dashboard-integration-design §138 ("Engines are not implemented (roadmap)"). |
| Productivity / wellness scores, qualitative insights, energy or mood trends | No persisted data columns support them. They would be invented metrics. |
| XP breakdowns by source / type / achievement | `StatisticsResponse` already declares these but the XP engine currently returns `Map.of()` stubs (XpServiceImpl.getStatistics). Not relied on; fixing them is XP-engine debt (§15). |
| Hour-of-day XP histogram | Derivable from `created_at` but marginal; deferred to §15 to keep the milestone proportional (Rule 30). |
| Longest streak over arbitrary windows / streak velocity | Only current + lifetime streak are persisted (`user_streaks`); derived windowing would require re-simulation of history. Deferred (§15). |

## 3. Existing Data Sources

| Source | Role |
| --- | --- |
| `tasks.status`, `tasks.completed_date`, `tasks.created_at` (V8) | Task completion time-series. |
| `task_time_entries.start_time`, `task_time_entries.duration_minutes`, `task_time_entries.user_id` (V8) | Focus-time time-series. |
| `goals.completed_date`, `goals.created_at`, `goals.status` (V7) | Goal completion time-series. |
| `xp_transactions.created_at`, `xp_transactions.amount`, `xp_transactions.user_id` (V9, immutable ledger ADR-0008) | XP earned time-series (positive amounts only). |
| `user_streak_history.activity_date`, `user_id` (V17) | Per-day activity time-series (persisted qualifying activity). |
| `xp_accounts` (V9) | Current XP / level / lifetime totals. |
| `user_streaks` (V17) | Current / longest streak. |
| `memories.type/importance/source/created_at` (V20) | Memory usage reporting. |
| `ai_interactions.provider/model/token columns/created_at` (V21) | AI usage reporting. |
| `user_profiles.timezone` (V5) | Day-bucketing zone via `UserTimezoneResolver` (same as StreakEngine and XP statistics). |
| Existing engine statistics DTOs | `TaskStatisticsResponse`, `GoalStatisticsResponse`, `StatisticsResponse`, `UserStreakResponse`, `XpAccountResponse` — composed into the overview. |

## 4. Decisions

### D1. Reporting engine — no analytics-owned tables
The Analytics Engine persists nothing. Rationale:

- **Rule 31** — "Each engine owns its data exclusively. **No duplicate data stores.**" An analytics table mirroring counts/series derived from engine tables would be a duplicate store of engine data.
- **Rule 30** — no premature abstraction; a read model earns its own schema only when it demonstrably needs it.
- **No event replay exists** — domain events are fire-and-forget in-process objects with no outbox/persistence. An event-fed analytics table could never be backfilled for existing users; on-demand queries cover the full history immediately.
- Design-doc precedent: goal-engine-design §13.3 explicitly states "Analytics Engine **queries** Goal Engine for historical data" — on-demand querying is the documented historical-data path.

### D2. Data access exclusively through owning-engine service interfaces (Rule 34)
Analytics never touches another engine's repository or entity. It injects the owning engines' **service interfaces** only. Cross-engine calls are one-directional: Analytics → {Task, Goal, XP, Memory, AI}. No cycle.

### D3. Two new read-only methods on owning engines
- `TaskService.listTimeEntriesForPeriod(UUID userId, Instant from, Instant to)` → `List<TimeEntryResponse>` (Task owns `task_time_entries`; query bounded by `start_time`).
- `XpService.getActivityTrend(UUID userId, LocalDate from, LocalDate to)` → `List<ActivityDay>` where `ActivityDay(LocalDate date, long count)` (XP owns `user_streak_history`; one row per distinct activity date).

Both are additive, read-only, per-user, and do not change existing behavior.

### D4. No new domain events this milestone
The engine is read-only. Event-driven incremental read models are deferred until an event bus / outbox exists (§15). This does not violate the "consumes events for reporting" design statements — those describe the future incremental path and the existing un-consumed events (`AiInteractionCreatedEvent`, `XpAwardedEvent`, etc.) remain the natural ingestion points then.

### D5. Timezone-aware day bucketing
Day boundaries for all series use `UserTimezoneResolver.resolveUserZoneId(userId)` — identical to XP statistics and the Streak Engine. All series are zero-filled for every day in the requested window so charts have a continuous axis.

### D6. Bounded look-back
`days` parameter on trends: default 14, minimum 1, maximum 90. Invalid values → `BAD_REQUEST` via validation.

### D7. Per-user scoping
Every analytics endpoint resolves the caller with `SecurityUtils.getCurrentUserId()` and passes it explicitly to every engine service method. userId is never read from the request. One user can never see another user's analytics.

### D8. Bucketing in the Analytics engine from DTO records
Series are computed from engine **DTO records** (Rule 7 — no entity leakage across boundaries). Data volumes per user are small (hundreds to low thousands of rows); in-Java bucketing is appropriate at personal-app scale (Rule 30; engine-owned SQL aggregation is §15 future work).

### D9. API surface
Four GET endpoints under `/api/v1/analytics` (read-only):

| Endpoint | Returns |
| --- | --- |
| `GET /api/v1/analytics/overview` | `AnalyticsOverviewResponse` — composed snapshot (XP, streak, task, goal, memory, AI usage). |
| `GET /api/v1/analytics/trends?days=14` | `AnalyticsTrendsResponse` — daily XP earned, tasks completed, goals completed, focus minutes, activity days, XP by day of week. |
| `GET /api/v1/analytics/ai-usage` | `AiUsageAnalyticsResponse` — interaction/token totals, averages, by-provider/model, per-day series. |
| `GET /api/v1/analytics/memories` | `MemoryUsageAnalyticsResponse` — totals, by type/importance/source, per-month series. |

## 5. Data Flow

```
GET /api/v1/analytics/trends?days=14
    │  SecurityUtils.getCurrentUserId() → userId
    ▼
AnalyticsService.getTrends(userId, days)
    │  resolve zone = UserTimezoneResolver(userId)        (D5)
    │  window = [today - (days-1), today] in zone
    ├─► XpService.getTransactionHistory(userId, from, to) ──► bucket amount>0 by day + day-of-week
    ├─► TaskService.listTasks(userId, noFilter)            ──► bucket status=COMPLETED by completedDate
    ├─► GoalService.getGoals(userId, noFilter)             ──► bucket status=COMPLETED by completedDate
    ├─► TaskService.listTimeEntriesForPeriod(userId, from, to) ──► sum durationMinutes by day
    └─► XpService.getActivityTrend(userId, from, to)       ──► activity count by day
    ▼
    zero-fill all days in window → AnalyticsTrendsResponse

GET /api/v1/analytics/ai-usage
    │  AiService.listInteractions(userId) → aggregate count/tokens, byProvider, byModel, per-day
GET /api/v1/analytics/memories
    │  MemoryService.listMemories(userId, noFilter) → aggregate byType/byImportance/bySource, per-month
GET /api/v1/analytics/overview
    │  XpService.getAccount + getUserStreak
    │  TaskService.getStatistics + GoalService.getStatistics
    │  + ai-usage and memory-usage aggregates (shared service methods)
```

All engine calls are `@Transactional(readOnly = true)` inside the owning engine; the Analytics engine is stateless.

## 6. API Dependencies

| Dependency | Type | Status |
| --- | --- | --- |
| `XpService.getAccount(UUID)`, `getUserStreak(UUID)`, `getStatistics()`, `getTransactionHistory(UUID, TransactionHistoryFilter)` | Service interface | Existing |
| `TaskService.getStatistics(UUID)`, `listTasks(UUID, TaskFilterRequest)` | Service interface | Existing |
| `TaskService.listTimeEntriesForPeriod(UUID, Instant, Instant)` | Service interface | **NEW** |
| `GoalService.getStatistics(UUID)`, `getGoals(UUID, GoalFilter)` | Service interface | Existing |
| `MemoryService.listMemories(UUID, MemoryFilterRequest)` | Service interface | Existing |
| `AiService.listInteractions(UUID)` | Service interface | Existing |
| `XpService.getActivityTrend(UUID, LocalDate, LocalDate)` | Service interface | **NEW** |
| `UserTimezoneResolver.resolveUserZoneId(UUID)` | User service | Existing |

Dependency graph remains acyclic: `analytics → task/goal/xp/memory/ai/user` (no engine depends on analytics).

## 7. DTO Changes

New Analytics module DTOs (records, all immutable):
- `AnalyticsOverviewResponse` — XP account fields, streak fields, task statistics, goal statistics, memory usage summary, AI usage summary.
- `AnalyticsTrendsResponse` — `List<DailyPoint>` for xpEarned / tasksCompleted / goalsCompleted / focusMinutes / activityDays, plus `Map<DayOfWeek, Long> xpByDayOfWeek`, plus `int days`, `LocalDate from`, `LocalDate to`.
  - `DailyPoint(LocalDate date, long value)`.
- `AiUsageAnalyticsResponse` — `long totalInteractions`, `long totalTokens`, `long promptTokens`, `long completionTokens`, `double avgTokensPerInteraction`, `Map<String, Long> byProvider`, `Map<String, Long> byModel`, `List<DailyPoint> perDay`.
- `MemoryUsageAnalyticsResponse` — `long totalMemories`, `Map<String, Long> byType`, `Map<String, Long> byImportance`, `Map<String, Long> bySource`, `List<MonthlyPoint> perMonth`.
  - `MonthlyPoint(String month, long count)` (`YYYY-MM`).

New owning-engine contract:
- `XpService` adds `record ActivityDay(LocalDate date, long count)` and `List<ActivityDay> getActivityTrend(UUID userId, LocalDate from, LocalDate to)` (same pattern as `TaskService.TaskGoalProgressSnapshot`).
- `TaskService` adds `List<TimeEntryResponse> listTimeEntriesForPeriod(UUID userId, Instant from, Instant to)` (reuses existing `TimeEntryResponse`).

No existing REST contract changes; existing endpoints/DTOs are untouched.

## 8. Authentication / Security

- All four endpoints require authentication (default-deny SecurityConfig; no path is added to the public allow-list).
- Caller identity comes only from `SecurityUtils.getCurrentUserId()`; userId is never accepted as input.
- Every engine service method is called with the caller's userId and the owning engine enforces ownership (`deletedAt IS NULL` scoping in every repository query).
- No sensitive data is logged; error responses use the standard `ApiResponse.error` envelope (Rule 13).

## 9. Empty-State Behavior

- User with no data at all → overview returns zeros and empty maps; trends returns a fully zero-filled series for the window; ai-usage/memories return zero counts and empty maps/`perDay`/`perMonth`.
- Null XP account (`getAccount` falls back to a zeroed account) → zeros.
- Missing streak row → current/longest streak 0 (existing `UserStreakResponse` semantics).
- `days` omitted → 14; `days` out of 1..90 → `BAD_REQUEST` (`VALIDATION_ERROR`-style via `BusinessException(BAD_REQUEST)`).

## 10. Partial-Failure Behavior

- If an engine service call fails, the exception propagates and the endpoint returns the standard error envelope via the module `AnalyticsExceptionHandler` (mapping `BusinessException` codes; generic failures → 500 `INTERNAL_ERROR`), consistent with other modules.
- Series are computed independently per engine; the whole response is all-or-nothing (no partial success) to keep the contract predictable at personal-app scale.

## 11. Performance Considerations

- Every cross-engine read is bounded by the look-back window (transactions: indexed `created_at`; time entries: bounded by `start_time`; streak history: bounded by `activity_date`; tasks/goals: full per-user list bucketed in Java).
- Day window capped at 90 (D6). Worst case per request is a few thousand rows across five engines — trivially fast for a single user.
- No analytics caching in this milestone; on-demand derivation keeps every response truthful (Rule 31). Caching/read-models are §15 future work.

## 12. Testing Strategy

- **Unit — `AnalyticsServiceImpl`** (mock all engine services): overview composition; trends bucketing per series (XP positive-only, completed-date filtering, focus-minute summation, activity counts); zero-filling across the full window; timezone boundary handling via a stubbed `UserTimezoneResolver`; day-of-week aggregation; AI/memory aggregation; `days` validation (reject 0, 91, negative).
- **Unit — `TaskServiceImpl.listTimeEntriesForPeriod`**: returns only rows in `[from, to]` by `start_time`, soft-deleted excluded, user-scoped.
- **Unit — `XpServiceImpl.getActivityTrend`**: distinct dates in range, counts, soft-deleted excluded, user-scoped; empty → empty list.
- **Integration — `AnalyticsControllerIntegrationTest`** (web-slice, mocked `AnalyticsService`, pattern from `MemoryControllerIntegrationTest`): each endpoint returns `success:true` envelope + correct status; validation error for bad `days`; 404-style `BusinessException` mapping.
- **Regression**: full backend suite (all existing module tests must stay green — the two new engine methods are additive).

## 13. Live Verification Strategy

1. Backend (:9000) + frontend (:9001) healthy (Flyway remains at v21 — no new migrations).
2. Seed a user; create tasks (some completed today/yesterday), a task with a time entry, a goal (one completed), memories of varying type/importance/source, and an AI interaction.
3. `GET /api/v1/analytics/trends?days=14` → verify daily series counts match the seeded rows (spot-check against the DB).
4. `GET /api/v1/analytics/ai-usage` and `/memories` → verify totals/breakdowns.
5. `GET /api/v1/analytics/overview` → verify composition.
6. `GET /api/v1/analytics/trends?days=0` and `days=91` → `BAD_REQUEST`.
7. Second user → empty/zero series; confirms per-user isolation.
8. Frontend `/analytics` page renders the new data; dashboard regression check.

## 14. Scope Boundaries

- Analytics Engine is **read-only**: no tables, no entities, no repositories, no migrations, no domain events, no writes.
- The only cross-engine surface changes are the two additive read-only methods (D3). No existing endpoint or DTO is modified.
- Metrics outside §2.2 (rejected list §2.3) are not implemented — no habit/sleep/health/calendar/integration analytics, no invented scores.
- No changes to the XP engine's stubbed breakdowns (debt, §15). No notification, habit, sleep, health, calendar, or integration engine work.
- Frontend: new Analytics page only; the existing Dashboard page is not modified.

## 15. Technical Debt / Future Work

- **XP breakdown stubs**: `StatisticsResponse.xpBySource / xpByType / xpByAchievement` are returned as `Map.of()` by `XpServiceImpl.getStatistics` — an XP-engine gap the Analytics engine does not work around.
- **Event-driven read models**: once an event bus / outbox exists (ADR-0009 was rejected as an internal bus), Analytics can maintain incremental summaries from `XpAwardedEvent`, `AchievementUnlockedEvent`, `AiInteractionCreatedEvent`, `MemoryCreatedEvent`, task/goal lifecycle events (the documented "consume events for reporting" path), replacing on-demand scans.
- **Engine-owned SQL aggregation**: move series bucketing into owning-engine aggregate queries when volumes demand it (Rule 30 — deferred).
- **Hour-of-day XP histogram** and **streak window re-simulation** (rejected §2.3) become feasible with the event bus.
- **Analytics caching** (short TTL) and, later, read replicas for cross-engine reporting.
- Dashboard `QuickFacts` currently fetches full memory/AI lists to count rows; it could consume `/api/v1/analytics/ai-usage` and `/memories` once the Analytics page ships (Dashboard itself is unchanged this milestone).
