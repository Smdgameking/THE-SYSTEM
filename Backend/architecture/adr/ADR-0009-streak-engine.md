# ADR-0009: Unified User Streak Engine

## Status

Accepted

## Date

2026-08-09

## Authors

THE SYSTEM Architecture Team

## Context

THE SYSTEM's XP Engine already supports streak-based multipliers through the `streak_bonus` policy condition. The existing policy seed data defines milestones `[3, 7, 14, 30, 60, 90]` with corresponding multipliers `[1.1, 1.25, 1.5, 2.0, 2.5, 3.0]`. The XP calculation path (`calculateMultiplierForPolicy`) already parses this configuration and can apply it when a `streak` value is present in the calculation context.

However, no authoritative global streak provider exists. The XP Engine has no way to determine a user's current streak, so the `streak_bonus` multiplier is never applied in practice.

Separately, the Task Engine already maintains per-task streak state for HABIT execution tasks inside `execution_state` JSONB. This is a task-specific habit tracker and must not be confused with a user-wide streak.

THE SYSTEM therefore needs a dedicated Streak Engine that:

* maintains one unified user-wide streak,
* counts qualifying activity from both Task and Goal engines,
* exposes the current streak to the XP calculation path,
* preserves the existing HABIT task streak as a separate concern.

## Problem Statement

How can THE SYSTEM introduce an authoritative global user streak that drives XP multipliers and future streak achievements, without conflating it with the existing per-task HABIT streak or coupling streak state to XP transaction history?

## Decision

**A dedicated Streak Engine, owned by the XP module, will maintain one unified user-wide streak derived from Task and Goal completion events.**

### 1. Unified Global Streak

THE SYSTEM maintains ONE user-wide streak.

Both completed tasks and completed goals are qualifying activities for the same streak. There are NOT separate task and goal streaks in v1.

### 2. Qualifying Activity

A qualifying activity is a Task or Goal transitioning to `COMPLETED`.

These do NOT qualify:

* failed tasks
* cancelled tasks
* manual XP adjustments
* rewards
* progress-only updates
* non-completion events

### 3. Daily Requirement

A user needs at least ONE qualifying activity during a calendar day for that day to be active. Multiple completions on the same day increase activity counts but advance the streak by only one day.

### 4. Streak Calculation

* First qualifying day → current streak becomes 1
* Consecutive active days → streak increments by 1 per active day
* One or more inactive days → current streak breaks
* First qualifying day after a break → current streak becomes 1
* `longest_streak` records the historical maximum
* There is no maximum streak length

### 5. Timezone

`UserProfile.timezone` is the source of truth for determining activity dates. The completion event's `occurredAt` timestamp is converted into that timezone before determining `activity_date`.

Rules:

* Day boundary = exactly 00:00:00 local time
* No grace period
* Null or invalid timezone → UTC fallback
* Historical activity dates are NOT rewritten when a user changes timezone
* Future completion events use the user's new timezone

### 6. Event-Driven History

The Streak Engine is event-driven. Authoritative streak state must NOT be reconstructed from `xp_transactions`. Dedicated streak state and history will be maintained by the Streak Engine. XP transactions and streak history are separate concerns.

### 7. Duplicate Events

Duplicate completion events must not create duplicate streak activity. The implementation must provide source-level idempotency so the same Task or Goal completion event cannot be counted twice.

### 8. Out-of-Order Events

The event's original `occurredAt` timestamp determines its activity date, not processing time. The implementation must support recalculating affected streak state when historical events arrive out of order.

### 9. HABIT Streak Separation

HABIT task streaks remain owned by the Task Engine. They represent the streak state of an individual HABIT task stored in `execution_state`.

The global user streak:

* is owned by the Streak Engine
* includes Task and Goal completions
* is independent of HABIT `execution_state.streak`

### 10. Streak Freeze

No streak-freeze mechanism exists in v1. Do not create freeze fields, APIs, policies, or implementation.

### 11. Manual Restoration

Users cannot manually restore a broken streak. Administrative restoration may be introduced in a future version and must be auditable. Do not implement restoration now.

### 12. Recurring / HABIT Behavior

Skipping a recurring or HABIT task has no special effect on the global streak. If another qualifying activity occurs that day, the day is active. If no qualifying activity occurs, that calendar day is inactive and the streak breaks.

### 13. XP Integration

The Streak Engine provides the authoritative current streak to the XP calculation path. The XP Engine uses the streak value as calculation context. The streak itself is NOT the multiplier. The XP policy determines the multiplier using the configured `streak_bonus` milestones. The existing 10.0x overall multiplier cap remains unchanged.

### 14. Streak Achievements

The Streak Engine will expose streak state and events so the achievement system can evaluate streak-based achievements. Detailed achievement requirement evaluation is outside this ADR and remains a separate implementation task.

## Alternatives Considered

### Separate Task and Goal Streaks

Rejected because the approved model defines one unified user activity streak.

### Deriving Streak from xp_transactions

Rejected because streak state should remain independent from XP transaction corrections and policy changes.

### Per-Task Global Streak

Rejected because HABIT execution streaks are task-specific and do not represent overall user consistency.

### Rolling 24-Hour Streak

Rejected because the system uses calendar-day semantics.

### Streak Freeze in v1

Rejected to keep the first implementation deterministic and minimal.

## Consequences

### Positive Consequences

* Clear ownership of streak state
* Deterministic calendar-day behavior
* Task and Goal activity unified under one streak
* Independent from XP transaction corrections
* Supports streak-based XP multipliers
* Supports future streak achievements
* HABIT functionality remains isolated

### Negative Consequences

* Dedicated persistence is required
* Timezone conversion becomes part of streak processing
* Out-of-order events require recalculation support
* Duplicate-event idempotency is required
* Timezone changes are intentionally not retroactive
* Achievement integration requires additional work

## Trade-offs

* **Gained**: Single authoritative streak, clean module boundaries, deterministic day semantics
* **Gave up**: Simplicity of deriving streak from existing XP transactions, retroactive timezone correction
* **Assumption**: Calendar-day semantics with strict midnight boundaries are acceptable for v1
* **Assumption**: Users will understand that changing timezone does not rewrite history
* **Assumption**: Event-driven streak history is worth the extra persistence overhead

## Future Impact

* Streak state becomes part of the public XP module contract
* All future XP sources must consider whether they qualify as streak activity
* Achievement engine will consume streak events
* Notification engine may consume streak events for milestones
* Analytics engine may consume streak events for reporting
* Timezone handling in other modules should eventually align with `UserProfile.timezone`

## References

* XP Engine Design: `architecture/xp-engine-design.md` (Section 8: XP Policies, Section 11: Task Integration, Section 12: Goal Integration, Section Streak Engine — Resolved Rules)
* Task Engine Design: `architecture/task-engine-design.md` (Section 13: Execution Providers)
* Backend Constitution: `architecture/backend-constitution.md`
* Database Constitution: `architecture/database-constitution.md`

---

*This ADR is part of THE SYSTEM Backend Architecture. All rules from the Backend Constitution apply.*
