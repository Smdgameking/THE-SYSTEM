# Goals Frontend Integration Design

## Version
1.1.0

## Date
2026-08-15

## Status
Accepted

---

## 1. Objective

Connect the completed Goal Engine (backend) into the authenticated application shell by replacing the `GoalsPage` placeholder with a real Goals page that consumes the existing `/api/v1/goals` contract. This is the first POST-AI integration milestone: it makes the Goal Engine usable from the frontend and completes the Goal → Task workflow that the UI currently cannot express (tasks reference `goal_id`, but the app cannot create a goal).

The milestone is primarily a frontend integration of verified backend contracts. During live verification two pre-existing backend defects that blocked goal creation were found and fixed (see §16); those are the only backend changes, both scoped strictly to making the Goal contract usable.

## 2. Scope

### In Scope

- New `Frontend/src/api/goalApi.js` client for `/api/v1/goals`.
- Real `Frontend/src/pages/GoalsPage.jsx` replacing the `ComingSoon` placeholder.
- New `Frontend/src/pages/goals.css` matching the existing visual language.
- Goal create, list (with status filter), detail, update, delete.
- Goal lifecycle transitions: start, pause, resume, complete, fail, archive.
- Goal progress update (percentage) and progress display.
- Goal statistics display.
- Milestone management: list, create, complete, update, delete.
- Loading / error / empty states; authentication-failure handling via existing `client.js`.
- Two pre-existing backend defect fixes that blocked goal create/update (see §16), each with a regression test.

### Out of Scope

- No new endpoints, services, migrations, or events.
- No changes to `GoalController`, the goal DTOs, or enums.
- No new engine. No cross-engine wiring (Goal ← Task progress remains future; ADR-0006 is Proposed).
- No dashboard changes, no XP/Settings frontend integration, no AI provider.
- No refactoring of the Goal Engine beyond the two defect fixes in §16.

## 3. Affected Engines

- **Goal Engine (backend)**: consumer only. Contract is unchanged.
- **Auth / User / Settings / Task / XP / Memory / AI**: unaffected.

## 4. Affected APIs

All consumed endpoints already exist under `/api/v1/goals`:

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/v1/goals` | Create goal |
| GET | `/api/v1/goals` | List goals (optional `status` filter) |
| GET | `/api/v1/goals/{id}` | Goal detail (includes milestones) |
| PUT | `/api/v1/goals/{id}` | Update goal |
| DELETE | `/api/v1/goals/{id}` | Soft delete goal |
| POST | `/api/v1/goals/{id}/start` | DRAFT → ACTIVE |
| POST | `/api/v1/goals/{id}/pause` | ACTIVE → PAUSED |
| POST | `/api/v1/goals/{id}/resume` | PAUSED → ACTIVE |
| POST | `/api/v1/goals/{id}/complete` | ACTIVE → COMPLETED |
| POST | `/api/v1/goals/{id}/fail` | ACTIVE → FAILED |
| POST | `/api/v1/goals/{id}/archive` | ARCHIVED (terminal) |
| PUT | `/api/v1/goals/{id}/progress?progress=NN` | Update progress |
| GET | `/api/v1/goals/statistics` | Goal statistics |
| GET | `/api/v1/goals/{id}/milestones` | List milestones |
| POST | `/api/v1/goals/{id}/milestones` | Create milestone |
| PUT | `/api/v1/goals/{id}/milestones/{mid}` | Update milestone |
| POST | `/api/v1/goals/{id}/milestones/{mid}/complete` | Complete milestone |
| DELETE | `/api/v1/goals/{id}/milestones/{mid}` | Delete milestone |

## 5. Frontend Flow

- User navigates to `/goals` (already routed and present in the sidebar).
- `GoalsPage` mounts → `GET /api/v1/goals` (list) + `GET /api/v1/goals/statistics` in parallel.
- Status filter tabs re-fetch the list (`?status=...`).
- "New Goal" opens the create form; "Edit" opens the form pre-filled from the row.
- Lifecycle actions call the corresponding transition endpoint, then reload the list.
- Selecting a goal loads its detail (milestones) and renders the milestone panel.
- All requests go through `client.js` (auth header, 401 refresh-retry, `ApiResponse` envelope handling).
- Errors surface via the standardized `success:false` + `error.message` path; the page never trusts HTTP status alone because `GoalExceptionHandler` maps `BusinessException` (including NOT_FOUND) to HTTP 400.

## 6. Backend Flow

Unchanged. The controller resolves `userId` via `SecurityUtils.getCurrentUserId()` and delegates to `GoalService`. User ownership/isolation is enforced server-side; cross-user access returns "Goal not found".

## 7. Events

None. This milestone neither publishes nor consumes domain events. (Goal Engine already publishes `GoalCreatedEvent`, `GoalCompletedEvent`, etc., and the XP/Streak engines already consume completion events; that behavior is unaffected.)

## 8. Security

- All requests are authenticated by the existing JWT filter; the page uses the shared `client.js`.
- The frontend never sends a `userId` — identity is resolved server-side.
- Unauthenticated access is redirected by `ProtectedRoute`; a 401 mid-session triggers the existing token-refresh flow in `client.js`.
- No secrets, tokens, or personal data are logged or stored beyond existing auth storage.

## 9. Persistence Changes

None. No migration is required. The two defect fixes (§16) alter runtime behavior only — IDs are now assigned before insert and tags are stored as a JSON array — no schema change.

## 10. Validation (Frontend)

Backend `CreateGoalRequest`/`UpdateGoalRequest` carry no bean-validation annotations, so the page enforces the documented Goal business rules itself:

- `title` required, 1–255 characters (trimmed).
- `description` ≤ 5000 characters.
- `category` ≤ 50 characters.
- `estimatedXp` ≥ 0, ≤ 100000.
- `progress` update clamped to 0–100.
- `displayOrder` for milestones ≥ 0.
- Blank milestone titles rejected.
- Server validation messages are still displayed if the backend rejects a payload.

## 11. Error Handling

- List/detail/statistics load errors: visible error banner with Retry.
- Action errors: inline banner with the backend `error.message`.
- Delete and archive/fail actions confirm via `window.confirm`.
- Loading spinners for initial load and busy states on action buttons.

## 12. Testing Strategy

- `npm run lint` must pass.
- `npm run build` must pass.
- Live verification against the running backend:
  - create goal → list → detail (milestones) → update → start → pause → resume → progress → complete → verify XP/streak side effects via `/api/v1/xp/streak` and account where observable.
  - milestone create/complete/delete.
  - fail/archive a goal.
  - delete a goal.
  - cross-user isolation (user B cannot read user A's goal → "Goal not found").
  - unauthenticated access to `/goals` API → 401.
- Full backend suite is run to confirm no regressions (baseline 498/0/0, plus 4 new regression tests → 502/0/0).

## 13. Non-Goals

- Not integrating XP/Progress or Settings frontends in this milestone.
- Not implementing Goal ← Task automatic progress (ADR-0006 Proposed).
- Not adding a Dashboard overhaul.
- Not fixing GoalExceptionHandler HTTP-status semantics or other discovered technical debt.

## 14. Future Extension Points

- `goalApi.js` becomes the single client used by the Dashboard and future pages.
- XP/Progress frontend integration can reuse the same page pattern.
- Goal ← Task progress, when ADR-0006 is accepted, will surface progress automatically without frontend changes beyond polling/refresh.

## 15. References

- Backend Constitution: Rules 3, 4, 7, 13, 14, 15, 21, 22, 27
- Module Constitution: Module structure and documentation rules
- Roadmap: Phase 5 (Goal module), Phase 7 (complete)
- Goal Engine Design: `architecture/goal-engine-design.md`
- Goal README: `docs/goal/README.md`
- Precedent: `TasksPage.jsx` + `taskApi.js` + `tasks.css`; `MemoriesPage.jsx` + `memoryApi.js`; `AiPage.jsx` + `aiApi.js`

## 16. Implementation Notes (v1.1.0)

Live verification against the running backend surfaced three issues that had to be resolved before the milestone could pass end-to-end.

### 16.1 Defect: Goal / milestone IDs were never generated

`Goal` and `GoalMilestone` were the only entities in the codebase without `@GeneratedValue` on `id`, and `GoalServiceImpl.createGoal`/`createMilestone` never assigned an id. Hibernate therefore inserted `NULL` for the primary key, and every goal/milestone create returned HTTP 500 (`INTERNAL_ERROR`). The defect was invisible to the 498-test suite because `GoalServiceUnitTest` mocks the repositories and `GoalControllerIntegrationTest` mocks the service — neither path hits a real database.

Fix: assign `UUID.randomUUID()` in `createGoal` and `createMilestone`, matching the existing pattern in the XP module (`XpServiceImpl`, `StreakEngine`). An attempt to add `@GeneratedValue` instead was rejected because it changes Spring Data `save()` to `merge()` semantics and broke the `StreakXpIntegrationTest` suite (`StaleObjectStateException` / "unsaved-value mapping was incorrect").

Regression coverage: new `GoalServiceIntegrationTest` (H2, real service + repositories) asserts generated ids for goals and milestones, and user scoping.

### 16.2 Defect: tags were written to a JSONB column as a plain comma string

`createGoal`/`updateGoal` stored tags via `String.join(",", request.tags())` (e.g. `"verify,integration"`), which is not valid JSON for the `tags jsonb` column → `invalid input syntax for type json` (SQLState 22P02) → HTTP 500 whenever tags were present.

Fix: write a proper JSON array via the service's existing `objectMapper` (`toJson(request.tags())`), and make `GoalMapper.stringToList` parse a JSON array with a comma-split fallback (backward compatible with any legacy comma-joined values). Regression coverage: `GoalServiceIntegrationTest.shouldCreateGoalWithTagsRoundTrippingJson`; also verified live on PostgreSQL with tags.

### 16.3 Frontend correction: `type` must not be sent on update

`UpdateGoalRequest` has no `type` field (unlike `CreateGoalRequest`). The first version of the Goals form spread the same payload on create and update; the backend treats the unknown `type` property as a deserialization failure that `GoalExceptionHandler` reports as HTTP 500 rather than 400. The form now omits `type` when editing. `GoalExceptionHandler`'s 500-vs-400 semantics for malformed bodies remains documented technical debt, not fixed here.

### 16.4 Remaining documented debt (unchanged)

- `GoalExceptionHandler` maps ALL `BusinessException` (including NOT_FOUND) to HTTP 400.
- `GoalExceptionHandler` reports malformed/unmappable bodies as HTTP 500 (generic `Exception` handler).
- `XpEventListener` injects `TaskRepository` directly (pre-existing cross-engine access).
- Goal `CreateGoalRequest`/`UpdateGoalRequest` carry no bean-validation annotations; the frontend enforces the documented rules.
- ADR-0006 (Goal ← Task progress) remains Proposed.
