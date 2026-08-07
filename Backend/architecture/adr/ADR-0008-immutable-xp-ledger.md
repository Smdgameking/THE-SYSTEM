# ADR-0008: Immutable XP Ledger Pattern

## Status

Proposed

## Date

2026-08-07

## Authors

THE SYSTEM Architecture Team

## Context

THE SYSTEM's XP Engine must track user effort and progress across multiple engines (Task, Goal, Health, etc.). XP represents earned progress and must be:
- Auditable (every change traceable)
- Immutable (history never overwritten)
- Consistent (current XP always matches transaction sum)
- Performant (fast reads for level/achievement checks)
- Extensible (new reward sources without schema changes)

The challenge is to balance audit requirements with performance while maintaining clean architecture and preventing cheating.

## Problem Statement

How can THE SYSTEM track XP changes with complete auditability while maintaining high performance and preventing manipulation?

Options considered:
1. Direct balance updates with audit log table
2. Event-sourced ledger with derived state
3. Hybrid approach with materialized view
4. Append-only ledger with cached balance

## Decision

**Append-only immutable ledger with cached derived state** is the selected approach.

### Core Principles

1. **XP Transactions are immutable**: Every XP change creates a new transaction record, never modifies existing ones
2. **Current XP is derived**: `xp_accounts` is a cache, not the source of truth
3. **Balance = Sum of transactions**: Current XP always equals SUM(xp_transactions.amount)
4. **Anti-cheat via idempotency**: Duplicate events detected and rejected
5. **Event-driven creation**: XP transactions created from domain events, never direct API calls

### Architecture

```
┌─────────────────┐     publishes      ┌──────────────────┐
│   Task Engine   │───────────────────▶│                  │
│                 │  TaskCompletedEvent │   XP Engine      │
└─────────────────┘                     │                  │
                                         │  - Validates     │
┌─────────────────┐     publishes      │  - Evaluates     │
│   Goal Engine   │───────────────────▶│    policies      │
│                 │  GoalCompletedEvent │  - Creates       │
└─────────────────┘                     │    transaction   │
                                         │  - Updates cache │
                                         │  - Publishes     │
                                         │    events        │
                                         └──────────────────┘
                                                 │
                                                 │ stores
                                                 ▼
                                         ┌─────────────────┐
                                         │ xp_transactions │
                                         │  (immutable)    │
                                         └─────────────────┘
                                                 │
                                                 │ derives
                                                 ▼
                                         ┌─────────────────┐
                                         │   xp_accounts   │
                                         │   (cached)      │
                                         └─────────────────┘
```

### Transaction Creation Flow

```
1. Domain event received (e.g., TaskCompletedEvent)
2. Validate event authenticity
3. Check idempotency (prevent duplicates)
4. Evaluate applicable XP policies
5. Calculate final XP amount (base + multipliers)
6. Create XP Transaction record (immutable)
7. Atomically update xp_accounts cache
8. Check for level up
9. Update achievement progress
10. Publish result events
```

### Anti-Cheat Mechanisms

- **Idempotency**: `(source_engine, source_id, source_type)` composite key prevents duplicate rewards
- **Time windows**: 5-minute tolerance for retries, 24-hour limit for historical replays
- **Balance verification**: Periodic reconciliation between xp_accounts and xp_transactions
- **Anomaly detection**: Unusual XP spikes flagged for review
- **Audit trail**: Every change fully traceable to source event

## Alternatives Considered

### Alternative 1: Direct Balance Updates with Audit Log

Rejected because:
- Balance updates can be modified or deleted
- Audit log can diverge from actual balance
- No guarantee of consistency
- Difficult to reconstruct history
- Vulnerable to direct manipulation

### Alternative 2: Event-Sourced Ledger with Derived State

Rejected because:
- Every read requires summing all transactions
- Performance degrades with transaction volume
- Complex snapshot management
- Over-engineered for read-heavy XP queries
- Violates Rule 30 (Engineering Over Speed)

### Alternative 3: Hybrid Approach with Materialized View

Rejected because:
- Materialized views add complexity
- Refresh timing creates consistency windows
- Additional infrastructure required
- Difficult to debug inconsistencies
- Adds operational burden

### Alternative 4: Append-Only Ledger with Cached Balance (Selected)

Accepted because:
- Immutable transactions provide complete audit trail
- Cached balance enables fast reads
- Atomic updates ensure consistency
- Simple to understand and debug
- Scales to millions of transactions
- Supports Rule 43 (One Source of Truth)
- Aligns with Rule 32 (Engine Ownership)

## Consequences

### Positive Consequences

- Complete audit trail for every XP change
- Current XP always consistent with transaction history
- Fast reads via cached xp_accounts
- Simple reconciliation via SUM query
- Easy to add new XP sources (just new transaction type)
- Prevents direct XP manipulation
- Supports analytics and reporting
- Enables rollback/forensic analysis

### Negative Consequences

- Two writes per transaction (ledger + cache)
- Cache invalidation complexity
- Requires periodic reconciliation
- More storage than simple balance table
- Initial implementation more complex

## Trade-offs

- **Gained**: Immutability, auditability, cheat resistance, scalability
- **Gave up**: Simplicity of direct updates, single-write performance
- **Assumption**: Two writes per transaction is acceptable overhead
- **Assumption**: Periodic reconciliation is sufficient for consistency
- **Assumption**: XP queries are read-heavy (justified by UX requirements)

## Future Impact

- All XP sources must emit events, never update balances directly
- Transaction schema becomes part of public API contract
- New engines can contribute to XP via events
- Audit requirements can be met without schema changes
- Prestige system can be added without modifying core pattern
- XP spending mechanics fit naturally (negative transactions)

## References

- Backend Constitution: Rule 32 (Engine Ownership), Rule 33 (One Source of Truth), Rule 43 (Audit Everything)
- XP Engine Design: `architecture/xp-engine-design.md` (Section 5: XP Transaction Model)
- Task Engine Design: `architecture/task-engine-design.md` (Section 17: Domain Events)
- Goal Engine Design: `architecture/goal-engine-design.md` (Section 10: Progress System)
- Database Constitution: `architecture/database-constitution.md` (Soft delete, audit fields)

---

*This ADR is part of THE SYSTEM Backend Architecture. All rules from the Backend Constitution apply.*
