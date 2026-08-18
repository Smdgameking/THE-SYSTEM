# Dashboard Frontend Integration Design

## Version
1.0.0

## Date
2026-08-15

## Status
Accepted

---

> The Dashboard does not own domain state. It presents information obtained from existing domain engines.

---

## 1. Purpose

Replace the static/invented `DashboardPage` placeholder with a trustworthy overview page that answers **"What is my current state in THE SYSTEM?"** using only real data obtained from existing, completed, JWT-authenticated backend engines (User, XP, Task, Goal, Memory, AI). This is the next integration milestone after the XP + Settings frontend milestone and is a **frontend-only aggregation layer** over existing contracts.

Every displayed value must be traceable through:

```
Dashboard UI
    ↓
existing frontend API client
    ↓
existing backend endpoint
    ↓
existing domain/service
    ↓
real persisted data
```

## 2. Current Dashboard Problems

`Frontend/src/pages/DashboardPage.jsx` is a static placeholder:

| Problem | Evidence |
|---------|----------|
| **Invented metric**: "Modules" = `5` | Hardcoded value at `DashboardPage.jsx:35`; not a domain metric, not derived from any API |
| **Invented status**: "System Status: Online" / "Authentication verified for this session" | `DashboardPage.jsx:26-32`; presented as system fact but derived from nothing at request time |
| No data fetching | The component performs zero API calls |
| Not an aggregation layer | Provides no view of the user's actual XP, tasks, goals, activity, memories, or AI interactions |

The page is "not a trustworthy representation of the system" and must be rebuilt on real data sources only.

## 3. Existing Data Sources

All endpoints below were verified against the live backend and source (`Backend/src/main/java/com/thesystem`). Every one is JWT-protected (`SecurityConfig`: `anyRequest().authenticated()`), and user scoping is derived exclusively from the JWT principal via `SecurityUtils.getCurrentUserId()` — no endpoint accepts a `userId` parameter, so no cross-user data is reachable.

| Engine | Endpoint | Response DTO (package `com.thesystem.modules.*`) | Fresh-user behavior |
|--------|----------|-----------------|---------------------|
| User | `GET /api/v1/users/me` | `user.dto.UserProfileResponse` — `{id, userId, username, displayName, bio, avatarUrl, timezone, locale, country, accountStatus, lastActiveAt, createdAt, updatedAt}` | Always present after login |
| XP | `GET /api/v1/xp/account` | `xp.dto.xpaccount.XpAccountResponse` — `{currentXp, currentLevel, totalXpEarned, totalXpSpent, lifetimeXp, levelProgress, ...}` | **404** `XP_ACCOUNT_NOT_FOUND` |
| XP | `GET /api/v1/xp/statistics` | `xp.dto.statistics.StatisticsResponse` — `{dailyXp, weeklyXp, monthlyXp, lifetimeXp, currentLevel, levelProgress, tasksCompleted, goalsCompleted, achievementsUnlocked, xpBySource, xpByType, xpByAchievement, generatedAt}` | 200 with zeros (`currentLevel=1`) |
| XP | `GET /api/v1/xp/streak` | `xp.dto.streak.UserStreakResponse` — `{currentStreak, longestStreak, currentStreakStartDate, lastActivityDate}` | **404** `USER_STREAK_NOT_FOUND` |
| XP | `GET /api/v1/xp/transactions?page=&size=` | `xp.dto.transaction.TransactionResponse` — `{id, transactionType, amount, balanceAfter, sourceEngine, sourceType, reason, createdAt, ...}` | 200 with `[]` |
| Task | `GET /api/v1/tasks/statistics` | `task.dto.TaskStatisticsResponse` — `{completionRate, totalTasks, completedTasks, failedTasks, cancelledTasks, archivedTasks, overdueTasks, averageCompletionTime, streakDays, focusTimeToday, focusTimeWeek, tasksByStatus, tasksByPriority, categoryBreakdown}` | 200 with zeros |
| Goal | `GET /api/v1/goals/statistics` | `goal.dto.GoalStatisticsResponse` — `{totalGoals, activeGoals, completedGoals, failedGoals, archivedGoals, averageCompletionPercentage, goalsByStatus, goalsByPriority}` | 200 with zeros |
| Memory | `GET /api/v1/memories` | `memory.dto.MemoryResponse` list | 200 with `[]` |
| AI | `GET /api/v1/ai/interactions` | `ai.dto.AiInteractionResponse` list | 200 with `[]` |

Response envelope for all: `common.response.ApiResponse<T>` = `{success, message, data, timestamp, requestId, error:{code,message}}`.

## 4. Candidate Metrics Considered

Per the milestone requirements, every candidate is scored on: (a) does an existing backend API provide it, (b) authenticated, (c) user-scoped, (d) stable contract, (e) consumable without backend changes, (f) no frontend duplication of business logic, (g) clear repository definition, (h) meaningful to the user, (i) real source of truth.

| Candidate | Source exists? | Verdict |
|-----------|----------------|---------|
| Current XP | `XP /statistics` (`lifetimeXp`, `dailyXp`) | **APPROVE** |
| Current level | `XP /statistics` (`currentLevel`) | **APPROVE** |
| Level progress | `XP /statistics` (`levelProgress`) | **APPROVE** |
| Current streak | `XP /streak` (`currentStreak`) | **APPROVE** (404-tolerant) |
| Longest streak | `XP /streak` (`longestStreak`) | **APPROVE** (404-tolerant) |
| Task counts / completed / overdue / rate | `Task /statistics` | **APPROVE** |
| Tasks by status | `Task /statistics` (`tasksByStatus`, zero-filled) | **APPROVE** (small, real breakdown) |
| Focus time today/week | `Task /statistics` (`focusTimeToday`, `focusTimeWeek`) | **APPROVE** (computed from real time entries; secondary display) |
| Active / completed goals | `Goal /statistics` | **APPROVE** |
| Avg goal completion % | `Goal /statistics` (`averageCompletionPercentage`) | **APPROVE** |
| Achievements unlocked | `XP /statistics` (`achievementsUnlocked`) | **APPROVE** |
| Recent transactions / activity | `XP /transactions` (top N) | **APPROVE** (only real "recent activity" feed) |
| Memory count | `Memory GET /memories` (list length) | **APPROVE** (client-derived from real list; no count endpoint exists) |
| AI interaction count | `AI GET /interactions` (list length) | **APPROVE** (client-derived from real list) |
| Task avg completion time | `Task /statistics` (`averageCompletionTime`) | **REJECT** — hardcoded `0.0` in `TaskServiceImpl.getStatistics` |
| Task streakDays | `Task /statistics` (`streakDays`) | **REJECT** — hardcoded `0`; real streak is `XP /streak` |
| XP breakdown maps | `XP /statistics` (`xpBySource/ByType/ByAchievement`) | **REJECT** — always `Map.of()` (empty) |
| Leaderboard rank | `XP /leaderboard` | **REJECT** — global endpoint; a user outside the fetched top-N has no rank, and `username` is a truncated user id; belongs on the Progress page |
| Achievement detail progress | `XP /achievements/user` | **REJECT** on dashboard (belongs to Progress page); only the unlocked count is shown |
| Charts / trends | none | **REJECT** — no time-series endpoint exists; daily/weekly/monthly are point-in-time, not series |
| Productivity score | none | **REJECT** — no engine exists (roadmap) |
| Health / sleep metrics | none | **REJECT** — no engine exists (roadmap) |
| Memory / AI content preview | list endpoints | **REJECT** — heavy; counts only are shown |
| "Modules: 5" card | none | **REJECT** — invented, removed |
| "System Status: Online" card | none | **REJECT** — invented, removed |

## 5. Approved Metrics

| Dashboard Metric | Source (existing API) | Displayed Field(s) | Verified |
|---|---|---|---|
| Greeting / operator | `GET /api/v1/users/me` (via `AuthContext.user`) | `displayName` / `username`, `accountStatus` | live |
| Current level | `GET /api/v1/xp/statistics` | `currentLevel` | live |
| Level progress | `GET /api/v1/xp/statistics` | `levelProgress` | live |
| Lifetime XP | `GET /api/v1/xp/statistics` | `lifetimeXp` | live |
| Daily XP | `GET /api/v1/xp/statistics` | `dailyXp` | live |
| Weekly XP | `GET /api/v1/xp/statistics` | `weeklyXp` | live |
| Current streak | `GET /api/v1/xp/streak` | `currentStreak` | live (404 → "—") |
| Longest streak | `GET /api/v1/xp/streak` | `longestStreak` | live (404 → "—") |
| Achievements unlocked | `GET /api/v1/xp/statistics` | `achievementsUnlocked` | live |
| Tasks total | `GET /api/v1/tasks/statistics` | `totalTasks` | live |
| Tasks completed | `GET /api/v1/tasks/statistics` | `completedTasks` | live |
| Tasks overdue | `GET /api/v1/tasks/statistics` | `overdueTasks` | live |
| Task completion rate | `GET /api/v1/tasks/statistics` | `completionRate` | live |
| Tasks by status | `GET /api/v1/tasks/statistics` | `tasksByStatus` (top entries) | live |
| Focus time today | `GET /api/v1/tasks/statistics` | `focusTimeToday` | live |
| Goals total | `GET /api/v1/goals/statistics` | `totalGoals` | live |
| Goals active | `GET /api/v1/goals/statistics` | `activeGoals` | live |
| Goals completed | `GET /api/v1/goals/statistics` | `completedGoals` | live |
| Avg goal completion % | `GET /api/v1/goals/statistics` | `averageCompletionPercentage` | live |
| Memory count | `GET /api/v1/memories` (client length) | array length | live |
| AI interactions count | `GET /api/v1/ai/interactions` (client length) | array length | live |
| Recent activity | `GET /api/v1/xp/transactions?page=0&size=8` | `transactionType`, `amount`, `reason`/`sourceType`, `createdAt` | live |

The XP statistics endpoint is the single source for level, XP amounts, and the achievements count (zeros-safe); `GET /xp/account` is **not** used because it 404s for fresh users — statistics already carries the same authoritative values with safe defaults.

## 6. Rejected Metrics and Why

| Metric | Reason for rejection |
|--------|----------------------|
| Task `averageCompletionTime` | Hardcoded `0.0` in `TaskServiceImpl.getStatistics` (line 468); displaying it would be fabricated data |
| Task `streakDays` | Hardcoded `0`; the authoritative user streak lives in the XP/Streak engine (`GET /xp/streak`) |
| XP `xpBySource` / `xpByType` / `xpByAchievement` | Always empty (`Map.of()` in `XpServiceImpl.getStatistics`); cannot power breakdowns or charts |
| Charts / trends | No time-series or history endpoint exists for the dashboard's scope; fake charts are prohibited |
| Leaderboard rank on dashboard | Global endpoint; user outside the fetched page has no rank; username is fabricated ("User <hex>"); kept on Progress page |
| Achievement detail progress | Redundant with Progress page; only the count is surfaced here |
| Productivity / health / sleep / analytics scores | Engines are not implemented (roadmap) |
| Static "Modules: 5" / "System Status: Online" cards | Invented values with no source chain; removed |

## 7. Data Flow

```
DashboardPage.jsx
    │  mount → Promise.all (parallel)
    ├── api/xpApi.getXpStatistics()          → /api/v1/xp/statistics
    ├── api/xpApi.getXpStreak()              → /api/v1/xp/streak          (404 tolerated)
    ├── api/xpApi.getXpTransactions(0,8)     → /api/v1/xp/transactions
    ├── api/taskApi.getTaskStatistics()      → /api/v1/tasks/statistics
    ├── api/goalApi.getGoalStatistics()      → /api/v1/goals/statistics
    ├── api/memoryApi.getMemories()          → /api/v1/memories           (count only)
    └── api/aiApi.getAiInteractions()        → /api/v1/ai/interactions    (count only)
        │
        ▼
   api/client.js (apiClient) → JWT header → 401 refresh-retry → ApiResponse envelope
        │
        ▼
   widget state slices (independent) → render
```

No new backend endpoint. No new API client module (existing `xpApi`, `taskApi`, `goalApi`, `memoryApi`, `aiApi` already expose every required call). Identity comes from `AuthContext.user` (already loaded by the auth provider via `GET /users/me`); the dashboard does not re-fetch it.

## 8. API Dependencies

| Client function (already exists) | Endpoint | Notes |
|----------------------------------|----------|-------|
| `xpApi.getXpStatistics()` | `GET /api/v1/xp/statistics` | zeros-safe; authoritative for level/XP/counts |
| `xpApi.getXpStreak()` | `GET /api/v1/xp/streak` | 404 `USER_STREAK_NOT_FOUND` → zero-state |
| `xpApi.getXpTransactions(0, 8)` | `GET /api/v1/xp/transactions?page=0&size=8` | top recent activity |
| `taskApi.getTaskStatistics()` | `GET /api/v1/tasks/statistics` | ignore `averageCompletionTime`/`streakDays` |
| `goalApi.getGoalStatistics()` | `GET /api/v1/goals/statistics` | |
| `memoryApi.getMemories()` | `GET /api/v1/memories` | full list; count used |
| `aiApi.getAiInteractions()` | `GET /api/v1/ai/interactions` | full list; count used |
| `useAuth().user` | `GET /api/v1/users/me` (already fetched by AuthProvider) | identity |

No changes to any backend controller, service, DTO, or migration. No `userId` is ever sent by the frontend; all scoping is server-side from the JWT.

## 9. Loading Strategy

- On mount, fetch all seven data slices in **parallel** with `Promise.all`.
- One `loading` gate for first paint (skeleton/loading state), matching `ProgressPage` precedent.
- Each slice resolves into its own state variable with a `success` flag so failures can be isolated.
- A "Refresh" button re-runs the same parallel load (bump a `reloadKey`), mirroring `ProgressPage.reload`.
- The memory/AI lists are used only for `length`; their content is not rendered.

## 10. Error Handling

- Reuse the established `errorMessage(result, fallback)` helper pattern (`result?.data?.error?.message || result?.data?.message || fallback`).
- Expected **404s** (`xp/streak`) are rendered as zero-states (`—`), not errors.
- If **all** primary slices fail (statistics + task statistics + goal statistics), show a full-page error state with Retry (the current `ProgressPage` pattern).
- **Partial failure**: each widget renders independently. A failed widget shows its own compact error note; unrelated widgets remain usable.

## 11. Authentication / Security

- Uses the existing `api/client.js` exclusively; no new auth mechanism, no manual token attachment (client adds `Authorization: Bearer` from stored tokens).
- Unauthenticated access is prevented by `ProtectedRoute` (route already wrapped).
- Expired access token → existing single-flight refresh-retry in `client.js` (401 → `/api/v1/auth/refresh` → retry).
- Logout clears tokens (`AuthContext.logout`) → `ProtectedRoute` redirects to `/`.
- No `userId` is sent; identity is server-side only → user isolation is guaranteed by every consumed endpoint (`findBy...AndUserIdAndDeletedAtIsNull`).
- No secrets, tokens, or personal data are logged or rendered beyond what the profile endpoint already returns.

## 12. Empty-State Behavior

- Fresh account (no XP, no tasks, no goals, no memories, no interactions): all zero-states render cleanly — level shows `1`, counts show `0`, streak shows `—`, recent activity shows the same empty message used on Progress ("No XP transactions yet…").
- The page must never fabricate data to fill emptiness.

## 13. Partial-Failure Behavior

Each widget is an independent slice:

- XP statistics fail → level/XP/achievements widgets show an inline "Couldn't load" note; tasks/goals/activity still render.
- Task statistics fail → task widget note; XP/goals/activity still render.
- Streak 404 → streak shown as `—` (expected).
- Memory or AI count fails → that single quick-fact card shows `—`; the rest of the dashboard unaffected.
- Global gate: only if XP statistics AND task statistics AND goal statistics all fail is the whole page replaced by the error state (prevents an all-blank dashboard).

## 14. Responsive / UI Behavior

- Uses the existing design language: `--sys-*` tokens, `page` / `page-header` / `page-title` / `page-subtitle` primitives, `page-card`, and the `xp-*` panel/button/badge/state classes from `progress.css` conventions.
- Layout: header (greeting + subtitle + Refresh), a **status strip** (level + level-progress bar, streak, lifetime/daily/weekly XP, achievements), a **tasks card** (total/completed/overdue/rate + by-status chips + focus time), a **goals card** (total/active/completed/avg completion), **quick facts** (memories, AI interactions), and **recent activity** (top 8 transactions).
- Responsive: grids collapse to a single column at `720px` (matching `progress.css`); the level card two-column layout stacks on narrow viewports.
- No charts are rendered (no real series data).

## 15. Performance Considerations

- Seven small requests in parallel; no waterfall.
- `GET /memories` and `GET /ai/interactions` return **unpaginated full lists** (current contract). The dashboard only needs their lengths, so the payload cost is acknowledged and bounded by the user's own data volume. Documented as future work (§19) to add count endpoints; until then a full-list fetch is the only correct way to count, and it is consistent with the existing Memories/AI pages.
- All data is rendered from memory; no persistence, no polling.
- Each `GET /users/me` (already performed by AuthProvider) bumps `lastActiveAt`; the dashboard reuses the cached `AuthContext.user` and does not call it again.

## 16. Testing Strategy

- **Backend**: no backend changes, so no new backend tests. Run the full Gradle suite and confirm the baseline stays green (currently **505 tests, 0 failures, 0 errors**).
- **Frontend**: `npm run lint` and `npm run build` must pass. The repository has no frontend test framework; stateful behavior is verified via live + visual verification instead (consistent with prior milestones).
- **Live verification** (§17) asserts `API response == displayed value` for every metric.

## 17. Live Verification Strategy

Run against the real backend (localhost:9000) + frontend dev server (localhost:9001):

1. Fresh user (register → login): dashboard shows level 1, zero counts, `—` streak, empty activity; compare against raw API responses.
2. User with tasks: create tasks (including a completed one) → dashboard task counts/rate match `GET /tasks/statistics`.
3. User with goals: create + start + complete a goal → goal counts match `GET /goals/statistics`.
4. User with XP/streak: completing tasks/goals awards XP and builds a streak → verify level/XP/streak values match `GET /xp/statistics` and `GET /xp/streak`; verify recent-activity rows match `GET /xp/transactions`.
5. Achievements: unlocked count matches `statistics.achievementsUnlocked`.
6. Multiple independent sources: toggle backend availability of one widget (e.g. fail one endpoint via an invalid state) to confirm partial-failure isolation.
7. Logout → redirect to login; re-login → dashboard reloads.
8. Unauthenticated API access → 401; `ProtectedRoute` redirects.
9. JWT refresh/expiry: shorten access-token life behavior verified via `client.js` single-flight refresh.
10. User isolation: user B's dashboard never reflects user A's data (server-side scoping already proven by the inventory).
11. `API response == Dashboard displayed value` for each metric in §5.

## 18. Scope Boundaries

### In Scope
- Rebuild `Frontend/src/pages/DashboardPage.jsx` on real data.
- New `Frontend/src/pages/dashboard.css`.
- Frontend-only aggregation using existing `xpApi`, `taskApi`, `goalApi`, `memoryApi`, `aiApi`, `client.js`, `useAuth`.
- Routes/nav verification (`/dashboard` already routed + sidebar entry + `SECTION_TITLES` present; no changes required).
- Documenting discovered defects without fixing them (§20).

### Out of Scope
- No backend changes of any kind (Option A — frontend-only aggregation; no new Dashboard engine, no new endpoint, no migration).
- No Habit / Sleep / Health / Analytics / Notification / Calendar / Integration.
- No Goal←Task progress (ADR-0006 remains Proposed).
- No new AI providers, no redesign of Memory / XP / Settings / auth.
- No charts, no invented metrics, no "coming soon" cards presented as data.
- No changes to ADRs unrelated to Dashboard.

## 19. Technical Debt / Future Work

- **Server-side count endpoints for Memory and AI** — today the dashboard derives counts from full-list fetches; dedicated count endpoints (or pagination metadata) would reduce payload and let these be lighter-weight widgets. Requires new backend work (out of scope here).
- `TaskStatisticsResponse.averageCompletionTime` and `streakDays` are hardcoded; until fixed they must not be displayed anywhere.
- XP `StatisticsResponse` breakdown maps (`xpBySource`, `xpByType`, `xpByAchievement`) are always empty; they cannot power charts until the service computes them.
- Leaderboard `username` is a truncated user id ("User <hex>"); a real username join is future work (documented in prior milestones).
- `GoalStatisticsResponse.averageCompletionPercentage` averages `completionPercentage` across all non-deleted goals (including drafts); interpretable but noted.
- Task/Goal list endpoints ignore `page`/`size` (unpaginated) — noted for the task/goal statistics call correctness, not a blocker for the dashboard.
- No frontend test framework exists; adding one (e.g. Vitest) is future work.

---

*This design follows the Goals and XP/Settings Frontend Integration milestone precedents and the Backend Constitution (Rules 27, 31, 32, 43) and ADR-0001/ADR-0005: the Dashboard is a presentation layer over existing engines and must not own domain state or duplicate engine business logic.*
