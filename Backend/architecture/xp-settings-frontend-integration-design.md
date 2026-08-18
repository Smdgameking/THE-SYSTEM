# XP + Settings Frontend Integration Design

## Version
1.0.0

## Date
2026-08-15

## Status
Accepted

---

## 1. Objective

Connect the completed XP Engine and Settings Engine (backend) into the authenticated application shell by replacing the `ProgressPage` and `SettingsPage` placeholders with real pages that consume the existing `/api/v1/xp` and `/api/v1/settings` contracts. This is the next integration milestone after the Goals frontend milestone: it makes XP gamification and user preferences usable from the frontend.

The milestone is primarily a frontend integration of verified backend contracts. Live verification exposed a small set of pre-existing Settings Engine defects that made the Settings contract unusable end-to-end (empty registry, missing UUID on setting creation); those are the only backend changes, scoped strictly to the Settings Engine (see §6, §12).

## 2. Scope

### In Scope

- New `Frontend/src/api/xpApi.js` client for `/api/v1/xp`.
- New `Frontend/src/api/settingsApi.js` client for `/api/v1/settings`.
- Real `Frontend/src/pages/ProgressPage.jsx` replacing the `ComingSoon` placeholder (XP account, statistics, streak, transactions, achievements, leaderboard).
- Real `Frontend/src/pages/SettingsPage.jsx` replacing the `ComingSoon` placeholder (registered user preferences grouped by namespace, type-aware controls).
- New `Frontend/src/pages/progress.css` and `Frontend/src/pages/settings.css` matching the existing visual language.
- Routes and nav wiring verification (`/progress`, `/settings` already routed and linked; no changes required).
- Two pre-existing Settings Engine defect fixes that blocked the Settings contract (see §12), each with a regression test.

### Out of Scope

- No new XP endpoints, services, migrations, or events.
- No changes to `XpController`, XP DTOs, or enums.
- No changes to `SettingsController` or settings DTOs (only the two defect fixes in §12 and startup registration of definitions).
- No reward-history UI (no `reward_history` controller endpoint exists today).
- No admin policy management UI (XP policy create/update/delete are admin-only and not part of a user-facing page).
- No dashboard changes, no Goal ← Task cross-engine wiring (ADR-0006 remains future), no other modules.
- Known debt deliberately not fixed here (see §13).

## 3. Affected Engines

- **XP Engine (backend)**: consumer only. Contract is unchanged.
- **Settings Engine (backend)**: consumer + two defect fixes + startup registration of definitions (all within the Settings module).
- **Auth / User / Goal / Task / Memory / AI**: unaffected.

## 4. Affected APIs

### 4.1 XP Engine — consumed endpoints (`/api/v1/xp`)

| Method | Endpoint | Purpose | Notes |
|--------|----------|---------|-------|
| GET | `/xp/account` | Current user XP account | 404 `XP_ACCOUNT_NOT_FOUND` when no account yet |
| GET | `/xp/statistics` | Per-user XP statistics | Returns zeros when no account |
| GET | `/xp/streak` | Current user streak | 404 `USER_STREAK_NOT_FOUND` when no streak |
| GET | `/xp/transactions?page=&size=` | Transaction list (page content) | Defaults page 0 / size 20 |
| GET | `/xp/achievements` | All achievement definitions | |
| GET | `/xp/achievements/user` | Current user achievements | |
| POST | `/xp/achievements/check` | Evaluate + unlock achievements | Used by "Refresh achievements" |
| GET | `/xp/leaderboard?page=&size=` | Leaderboard entries + paging | Self-highlight via `userId` |

Response shapes (records): `XpAccountResponse`, `StatisticsResponse`, `UserStreakResponse`, `TransactionResponse`, `AchievementResponse`, `UserAchievementResponse`, `LeaderboardResponse`/`LeaderboardEntry`. Error envelope is `{success, error:{code,message}}` via `XpControllerAdvice` (status-preserving).

### 4.2 Settings Engine — consumed endpoints (`/api/v1/settings`)

| Method | Endpoint | Purpose | Notes |
|--------|----------|---------|-------|
| GET | `/settings/definitions/engine/{engine}` | All registered definitions for an owning engine | Single source of truth for what to render |
| GET | `/settings/{namespace}` | User's explicit overrides in a namespace | Empty map if none set |
| PUT | `/settings/{namespace}/{key}` | Set a user setting (body `{"value": ...}`) | |
| DELETE | `/settings/{namespace}/{key}` | Revert a setting to default | |
| POST | `/settings/{namespace}/reset` | Reset a namespace to defaults | |

`SettingDefinitionResponse` exposes `{namespace, key, type, defaultValue, description, visibility, owningEngine}`. `SettingResponse` exposes `{namespace, key, value, type, description, isSystem, updatedAt}` where `value` is a **string** (stored form); the frontend coerces to boolean/number using `type`. Definitions endpoint is JWT-protected but not ownership-scoped (safe for any authenticated user).

## 5. Integration Gap Report

### 5.1 XP Engine — READY

All endpoints needed by a Progress page exist, are JWT-protected, and follow the shared envelope. Two behaviors the page must tolerate:
- `GET /account` and `GET /streak` return 404 (not zeros) for a user with no XP data.
- `GET /statistics` and `GET /transactions` are always safe (zeros / empty).

No backend work required for XP.

### 5.2 Settings Engine — BLOCKED, minimal fixes required

| # | Gap | Evidence | Impact |
|---|-----|----------|--------|
| G1 | **Registry is empty at startup** — no engine registers definitions, and the registry is never locked | `registerDefinition` has no callers; no `new SettingDefinition(...)` anywhere in `src/main`; no `lock()` invocation | Every `GET/PUT /settings/{namespace}/{key}` throws `Setting not registered` → Settings page is non-functional |
| G2 | **New settings are created without an id** | `SettingsServiceImpl.setSetting` / `setSystemSetting` build `new Setting()` without `setId(...)`; `Setting` has `@Id UUID id` with no generator | First save of any setting → Hibernate "null id generated" → HTTP 500 (same defect class as the Goals UUID bug fixed in the previous milestone) |
| G3 | **No default fallback on read** | `getSetting`/`getNamespaceSettings` only return rows that exist | Frontend must merge `defaultValue` from definitions with user overrides; acceptable, no backend change |

G1 and G2 are in-scope backend fixes (§12). G3 is handled in the frontend (design choice, no backend change).

## 6. Backend Changes (Settings Engine only)

1. **`SettingsDefinitionRegistrar`** — new `@Component` in `com.thesystem.modules.settings.config`. On startup (`@PostConstruct`, i.e. during context initialization, before the web server accepts requests per Rule 40) it registers the canonical set of PUBLIC user preferences via `SettingRegistry.registerAll(...)` and then `SettingRegistry.lock()`. Registered set (owning engine `settings`, visibility `PUBLIC`):

   | Namespace | Key | Type | Default | Description |
   |-----------|-----|------|---------|-------------|
   | appearance | theme | STRING | `light` | UI theme preference |
   | appearance | timezone | STRING | `UTC` | Timezone used for XP/activity dates |
   | notification | enabled | BOOLEAN | `true` | Master toggle for in-app notifications |
   | xp | levelUpNotifications | BOOLEAN | `true` | Notify when reaching a new level |
   | xp | goalCompletionNotifications | BOOLEAN | `true` | Notify when a goal is completed |

   This follows the design doc's canonical example set (`settings-engine-design.md` §7.2) and the XP README ("Settings Engine: Stores user preferences for XP display, level notifications").

2. **`SettingsServiceImpl.setSetting` / `setSystemSetting`** — assign `s.setId(UUID.randomUUID())` on the create path (G2).

## 7. Frontend Design

### 7.1 `Frontend/src/api/xpApi.js`

Thin wrappers over `apiClient` mirroring `goalApi.js`: `getXpAccount`, `getXpStatistics`, `getXpStreak`, `getXpTransactions(page,size)`, `getAchievements`, `getMyAchievements`, `checkAchievements`, `getLeaderboard(page,size)`.

### 7.2 `Frontend/src/api/settingsApi.js`

`getDefinitionsByEngine(engine)`, `getNamespaceSettings(namespace)`, `setSetting(namespace,key,value)`, `deleteSetting(namespace,key)`, `resetNamespace(namespace)`.

### 7.3 `Frontend/src/pages/ProgressPage.jsx` (XP)

Data fetched on mount (parallel): account, statistics, streak, transactions (size 15), achievements (all + mine), leaderboard (size 10). Tolerant loaders for the 404s (account/streak → zero-state). Layout:

- **Page header** — title/subtitle + "Refresh achievements" action (`POST /achievements/check` then reload).
- **Stats strip** — daily / weekly / monthly XP, tasks & goals completed, achievements unlocked (from `statistics`).
- **Level card** — level, `levelProgress` bar, current vs lifetime XP, streak (current/longest) when present.
- **Recent transactions** — latest 15: type (title-cased), signed amount with gain/loss color, reason/source, relative date.
- **Achievements** — merge definitions with per-user progress (`achievementId` match): progress bar `currentProgress/targetProgress`, unlocked badge, hidden-but-unlocked only.
- **Leaderboard** — top 10, rank/level/XP, highlight own row via `account.userId`.

### 7.4 `Frontend/src/pages/SettingsPage.jsx`

- Fetch all definitions for engine `settings` (`GET /definitions/engine/settings`), filter `visibility === 'PUBLIC'`, group by namespace; fetch each namespace's user overrides.
- Render one section per namespace (labeled Appearance / Notifications / XP) with a description line and a "Reset to defaults" button (`POST /{namespace}/reset`).
- Per definition, a type-aware control: BOOLEAN → toggle; STRING → text input; INTEGER/DOUBLE → number input; ENUM → text (allowed values not exposed by the contract today).
- Effective value = user override (`value` coerced by `type`) else `defaultValue`. Changes save immediately (`PUT /{namespace}/{key}`), with per-row saving/error/success feedback.
- Empty definitions (e.g., backend without registrations) → clear empty state instead of a crash.

### 7.5 Routes and nav

Already wired: `/progress` and `/settings` routes exist in `App.jsx`, sidebar entries exist in `Sidebar.jsx`, `TopBar` titles exist in `AppShell.jsx`. No changes required (verified in §10).

### 7.6 Styling

New `progress.css` and `settings.css` following the `goals.css` conventions: `--sys-*` design tokens, panel/card styling, same button/state/tab patterns, responsive breakpoints at 720px.

## 8. Error Handling

Reuse the established pattern: `errorMessage(result, fallback)` reading `result?.data?.error?.message`. 404s on account/streak are expected and rendered as zero-states, not errors. All mutation calls reload the relevant slice after success.

## 9. Testing Strategy

- **Backend**: add a unit test for the registrar (definitions registered + registry locked + PUBLIC visibility) and a unit test for the id fix (new `Setting` persisted by `setSetting` carries a non-null UUID). Run the full Gradle suite.
- **Frontend**: `npm run lint` and `npm run build`.
- **Live verification**: scripted curl flow against the running backend (register → login → XP reads → settings GET definitions / PUT / namespace reset), then manual browser check of both pages.

## 10. Verification of Existing Wiring

- `App.jsx` → routes `/progress` and `/settings` already present under `ProtectedRoute`/`AppShell`.
- `Sidebar.jsx` → "Progress" (TrendingUpIcon) and "Settings" (SettingsIcon) already present.
- `AppShell.jsx` → `SECTION_TITLES` already contains `/progress` and `/settings`.
- No edits needed; the milestone's "routes/nav" item is satisfied by verification.

## 11. Deliverables

- `Backend/architecture/xp-settings-frontend-integration-design.md` (this document)
- `Backend/src/main/java/com/thesystem/modules/settings/config/SettingsDefinitionRegistrar.java`
- `Backend/src/main/java/com/thesystem/modules/settings/service/impl/SettingsServiceImpl.java` (2-line id fix)
- Backend regression tests for the above
- `Frontend/src/api/xpApi.js`, `Frontend/src/api/settingsApi.js`
- `Frontend/src/pages/ProgressPage.jsx`, `Frontend/src/pages/progress.css`
- `Frontend/src/pages/SettingsPage.jsx`, `Frontend/src/pages/settings.css`

## 12. Backend Defect Fixes (with rationale)

| Fix | Location | Reason |
|-----|----------|--------|
| Register definitions + lock at startup | `SettingsDefinitionRegistrar` | Satisfies Rules 38/39/40 and ADR-0004 (registration is mandated but never wired); required for the Settings page to function |
| `setId(UUID.randomUUID())` on create | `SettingsServiceImpl.setSetting` + `setSystemSetting` | Prevents `null id generated` 500 on first write (G2) |

## 13. Known Debt (NOT addressed)

- `SettingsExceptionHandler` maps every `BusinessException` to HTTP 400 (NOT_FOUND/FORBIDDEN lose their status) — same defect class as the Goals handler.
- No default-value fallback server-side (`getSetting` 404s until a row exists); frontend merges defaults from definitions instead.
- `SettingResponse.value` returns the stored string form; type coercion is a frontend concern.
- XP `getStatistics` breakdown maps (`xpBySource`, `xpByType`, `xpByAchievement`) are always empty.
- Leaderboard `username` is a truncated user id ("User <8 hex>") — no user-name join.
- Malformed bodies on settings PUT → 500 (no bean validation on `SetSettingRequest`).

---

*This design follows the Goals Frontend Integration milestone precedent and the Backend Constitution (Rules 32, 38, 39, 40, 43).*
