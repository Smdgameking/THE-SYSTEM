# Goal ← Task Progress Integration Design

## Version
1.0.0

## Date
2026-08-15

## Status
Accepted

---

> For `TASK_BASED` goals, progress is **derived** from linked tasks. The Task table is the source of truth; `goals.current_progress` / `goals.completion_percentage` are derived caches.

---

## 1. Purpose

Implement ADR-0006 (Accepted) for the `TASK_BASED` completion strategy: Goal progress and completion must be automatically recomputed from the user's linked tasks whenever tasks are created, updated, deleted, or completed. This replaces any manual/stale progress on task-based goals with an event-driven, derived value that always reflects the current task state.

## 2. Current Problems

1. Goal progress is only recalculated for `MILESTONE_BASED` goals (`GoalServiceImpl.recalculateProgress`). `TASK_BASED` goals are never derived from tasks.
2. Completing, deleting, or un-linking a task does not advance or correct any goal's progress.
3. The existing task event payloads (`TaskCreatedEvent`, `TaskUpdatedEvent`, `TaskDeletedEvent`) carry lossy `Long` IDs, so the Goal Engine cannot reliably identify the affected goal.
4. A `TASK_BASED` goal's progress can still be manually overridden via `updateProgress` / `completeGoal`, producing a contradiction between stored progress and actual task state.

## 3. Existing Data Sources

| Source | Role |
| --- | --- |
| `tasks.goal_id` (V8) | Direct single-goal link. One task ↔ one goal. |
| `tasks.status`, `tasks.deleted_at` | Completion state + soft-delete. |
| `goals.completion_strategy` | Strategy selection (`TASK_BASED` and others). |
| `goals.current_progress`, `goals.completion_percentage` | Derived cache, recomputed by this milestone. |
| `TaskCompletedEvent(taskId, userId, goalId, ...)` | Already UUID-typed and carries `goalId`. |
| `TaskCreatedEvent` / `TaskUpdatedEvent` / `TaskDeletedEvent` | Enriched to UUID + `goalId` (this milestone). |
| `GoalProgressUpdatedEvent`, `GoalCompletedEvent` | Published by Goal Engine on change (existing consumers: XP, Streak). |

## 4. Decisions

### D1. Relationship model
One task belongs to exactly one goal (`tasks.goal_id`, direct column). Confirmed by existing schema, `TaskRepository.findByUserIdAndGoalIdAndDeletedAtIsNull`, `TaskFilterRequest.goalId`, and `TaskResponse.goalId`. No join entity, no many-to-many.

### D2. Derivation rule (TASK_BASED)
Progress = `round(completed_non_deleted_tasks / total_non_deleted_tasks * 100)`.
- `total` = tasks with matching `userId`, `goal_id`, and `deleted_at IS NULL`.
- `completed` = subset of `total` with `status = COMPLETED`.
- Empty set → progress `0`, percentage `0.0`.

### D3. Event wiring
Goal Engine subscribes (via a new `GoalTaskProgressListener`) to four Task Engine events:
- `TaskCreatedEvent` — task enters a goal (denominator grows).
- `TaskUpdatedEvent` — status/goal changes; carries `previousGoalId` so the previous goal is recalculated too.
- `TaskDeletedEvent` — task leaves a goal (denominator and/or numerator shrink).
- `TaskCompletedEvent` — task becomes completed (numerator grows).

All four events become UUID-typed and carry `goalId`. `deleteSubtask` additionally publishes `TaskDeletedEvent` (a subtask can be goal-linked). A new `TaskService.getGoalTaskProgress(userId, goalId)` returns `(completedCount, totalCount)` so the Goal Engine never touches Task repositories (Rule 34).

### D4. Recalculation semantics
- Recalc is invoked from `GoalServiceImpl.recalculateProgress`, which already exists; a `TASK_BASED` branch is added.
- Reaching 100% with ≥1 task → `status = COMPLETED`, `completedDate = now`, publishes `GoalCompletedEvent` (existing XP/Streak consumers react as today).
- Regressing below 100% while `COMPLETED` → `status = ACTIVE`, `completedDate = null` (truthful derivation). XP already awarded for the goal is **not** retracted (see §19).
- `FAILED` / `ARCHIVED` goals are never touched by task events; `deleted` goals are skipped.
- Idempotent: count-based, so duplicate or out-of-order events converge to the same state.

### D5. Manual override guard
- `updateProgress` (manual) → rejected with `ErrorCodes.BAD_REQUEST` for `TASK_BASED` goals.
- `completeGoal` (manual) → rejected with `ErrorCodes.BAD_REQUEST` for `TASK_BASED` goals.
- Switching strategy to `TASK_BASED` via `updateGoal` triggers an immediate recalculation.

### D6. Other strategies
`MANUAL`, `MILESTONE_BASED`, `XP_BASED`, `PERCENTAGE`, `CUSTOM` are unchanged in this milestone. Only `TASK_BASED` is derived from tasks.

### D7. Execution model
Synchronous in-process Spring events, consistent with the existing `XpEventListener` / `StreakEventListener` consumers. The event dispatch stays inside the publishing transaction, so a listener querying the task repository observes the just-persisted state (JPA auto-flush). ADR-0006's "asynchronous" principle is honored by decoupling through events (no cross-engine blocking service calls in the happy path); a true async bus is future work (see §19).

### D8. API surface
No new endpoints, no backend DTO changes. The frontend reuses `GET /api/tasks?goalId=...` (via the existing `getTasks` client) to render linked-task summaries per goal.

## 5. Data Flow

```
Task Engine                          Goal Engine
────────────                         ───────────
createTask ──► TaskCreatedEvent ────────► GoalTaskProgressListener
updateTask ──► TaskUpdatedEvent ─────────►  (event carries userId+goalId)
deleteTask ──► TaskDeletedEvent ─────────►      │
deleteSubtask ─► TaskDeletedEvent ─────────     ▼
completeTask ─► TaskCompletedEvent ────► GoalService.recalculateProgress(userId, goalId)
                                             │
                                             ▼
                              TaskService.getGoalTaskProgress(userId, goalId)
                                             │  (count-based)
                                             ▼
                              Update goals.current_progress / completion_percentage
                              / status / completedDate
                                             │
                                             ▼
                              Publish GoalProgressUpdatedEvent / GoalCompletedEvent
                                             │
                                             ▼
                              (existing XP / Streak consumers)
```

## 6. API Dependencies

- `TaskService.getGoalTaskProgress(UUID userId, UUID goalId)` — new cross-engine service method (Goal → Task interface, one-directional; no cycle).
- No change to `GoalController`, `TaskController`, or any REST contract.
- Frontend: existing `taskApi.getTasks` with `goalId` filter.

## 7. DTO Changes

- Task events: `TaskCreatedEvent`, `TaskUpdatedEvent`, `TaskDeletedEvent` become UUID-typed records carrying `goalId` (and `previousGoalId` for updated). `TaskCompletedEvent` is unchanged.
- Backend goal/task REST DTOs: none changed.
- Frontend: GoalsPage renders a linked-task chip (`completed/total`) from the task list grouped by `goalId` — no new API contract.

## 8. Authentication / Security

- All REST flows still resolve the caller via `SecurityUtils.getCurrentUserId()`; userId is never trusted from the request body.
- Task events carry the owner `userId`; the Goal listener scopes every goal lookup by `userId` (`findByIdAndUserIdAndDeletedAtIsNull`), so one user's events can never mutate another user's goals.

## 9. Empty-State Behavior

- A `TASK_BASED` goal with no linked tasks → progress `0%`, not complete.
- Goal events for a goal that was deleted or has a non-TASK_BASED strategy are ignored.

## 10. Partial-Failure Behavior

- Missing/deleted goal or non-TASK_BASED strategy: listener no-ops.
- Recalc failures are logged and do not break the originating task operation (listener exceptions propagate and roll back the transaction with the task change — consistent with existing listeners).
- Duplicate/out-of-order events: count-based recalc is idempotent.

## 11. Performance Considerations

- Recalc cost is O(tasks in the goal) per event via one indexed query (`findByUserIdAndGoalIdAndDeletedAtIsNull`).
- Frontend fetches tasks once and groups by `goalId` client-side (avoids N+1).
- Full event-bus / async processing deferred (§19).

## 12. Testing Strategy

- **Unit — TaskService**: `getGoalTaskProgress` returns correct counts (completed, total, soft-deleted excluded).
- **Unit — GoalService**: `recalculateProgress` for TASK_BASED (0%, partial, 100% auto-complete, regression reverts to ACTIVE), manual-override rejection for TASK_BASED, strategy-switch recalc, other strategies untouched.
- **Unit — GoalTaskProgressListener**: each of the four events routes to recalc for the right goal; skips null goalId, missing goal, non-TASK_BASED, FAILED/ARCHIVED, deleted goals; user isolation.
- **Integration**: create TASK_BASED goal → create 2 linked tasks → complete 1 (goal 50%) → complete 2nd (goal 100%, COMPLETED, `GoalCompletedEvent`) → delete a task (goal regresses); assert persisted DB state and published events.
- **Regression**: existing XP/Streak listeners still consume the enriched events (full suite).

## 13. Live Verification Strategy

1. Run backend (:9000) + frontend (:9001) and confirm health.
2. Seed a user; create a `TASK_BASED` goal via API.
3. Create two tasks with `goalId`; complete one → `GET /api/goals/{id}` shows 50%.
4. Complete the second → goal `COMPLETED`, 100%; verify XP/streak side effects (existing behavior).
5. Delete a linked task → goal progress recalculates downward.
6. Attempt `updateProgress`/`completeGoal` on the TASK_BASED goal → `BAD_REQUEST`.
7. Verify another user's goal is unaffected.
8. Frontend: GoalsPage shows the linked-task summary.

## 14. Scope Boundaries

- Only the `TASK_BASED` strategy is automated. `XP_BASED`, `PERCENTAGE`, `CUSTOM`, `MILESTONE_BASED`, `MANUAL` are untouched.
- No new DB columns, no new REST endpoints, no task-engine UI changes.
- No retraction of XP/streak effects when a completed goal regresses.
- No analytics, notifications, or other roadmap engines.

## 15. Technical Debt / Future Work

- `XP_BASED` progress derivation (deferred — ADR-0006 pattern applies later).
- True asynchronous event bus with retries/outbox (ADR-0009 rejected as an internal bus; revisit via an infrastructure decision).
- Retract or ledger-adjust XP/streak when a derived goal regresses below 100%.
- Link tasks to goals from the frontend task form (API support already exists).
- `TaskFailedEvent` / `TaskSubtask*Event` still use `Long` IDs (same debt class as the events fixed here); normalize when a consumer needs them.
- Manual override guard for other derived strategies once they are implemented.
