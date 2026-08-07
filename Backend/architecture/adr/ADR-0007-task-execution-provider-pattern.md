# ADR-0007: Task Execution Provider Pattern

## Status

Proposed

## Date

2026-08-07

## Authors

THE SYSTEM Architecture Team

## Context

THE SYSTEM's Task Engine must support diverse kinds of work. Not all tasks are simple "done/not-done" actions. Some tasks require multiple steps (checklists), time investment (timers), numeric targets (counts), gradual progress (progress bars), recurring behavior (habits), or external validation (approvals).

The challenge is to enable the Task Engine to handle these different execution models without:
- Large switch/case statements scattered throughout the codebase
- Tight coupling between execution logic and task lifecycle
- Difficulty adding new execution types in the future
- Inconsistent validation and progress calculation across types
- External engines implementing task execution logic

## Problem Statement

How can THE SYSTEM enable the Task Engine to support multiple execution models while maintaining clean architecture, extensibility, and strict engine boundaries?

Options considered:
1. Single task table with nullable execution-specific columns
2. Single execution_type column with switch/case logic
3. Table-per-execution-type inheritance
4. Execution Provider pattern with strategy objects

## Decision

**Execution Provider pattern with strategy objects** is the selected approach.

### Core Principles

1. **Task Engine owns execution behavior**: No external engine may implement task execution logic (Rule 42)
2. **Each execution type has a dedicated provider**: Encapsulates validation, progress calculation, and completion rules
3. **Execution State is structured JSONB**: Not a simple flag, but a complete runtime representation
4. **Providers are pluggable**: New execution types added via registry, no core logic changes
5. **Task Engine coordinates**: Owns lifecycle, delegates execution to providers

### Architecture

```
┌─────────────────┐         uses          ┌──────────────────────┐
│   Task Engine   │◀──────────────────────│ ExecutionProvider    │
│                 │                       │   Interface          │
│ - Lifecycle     │                       ├──────────────────────┤
│ - Dependencies  │                       │ BooleanProvider      │
│ - Scheduling    │                       │ ChecklistProvider    │
│ - Time Tracking │                       │ TimerProvider        │
│ - Recurrence    │                       │ CountProvider        │
│                 │                       │ ProgressProvider     │
│                 │                       │ HabitProvider        │
│                 │                       │ ApprovalProvider     │
│                 │                       │ CustomProvider       │
└─────────────────┘                       └──────────────────────┘
        │
        │ stores
        ▼
┌─────────────────┐
│ execution_state │
│     JSONB       │
└─────────────────┘
```

### Execution Provider Interface

```java
public interface ExecutionProvider {
    ExecutionState getInitialState();
    ExecutionState calculateProgress(ExecutionState currentState, TaskContext context);
    boolean isComplete(ExecutionState state);
    void validate(ExecutionState state);
    ExecutionType getType();
}
```

### Execution State

Execution State is stored as JSONB in the `execution_state` column. Each execution type defines its own schema:

**BOOLEAN**
```json
{ "completed": true }
```

**CHECKLIST**
```json
{ "items": [...], "completedItems": 1, "totalItems": 2 }
```

**TIMER**
```json
{ "targetMinutes": 120, "completedMinutes": 95, "sessions": 4, "paused": false }
```

**COUNT**
```json
{ "target": 100, "completed": 62, "unit": "pages" }
```

**PROGRESS**
```json
{ "percentage": 67 }
```

**HABIT**
```json
{ "currentStreak": 14, "bestStreak": 30, "lastCompletion": "...", "frequency": "DAILY" }
```

**APPROVAL**
```json
{ "submitted": true, "approved": false, "approvedBy": null }
```

**CUSTOM**
```json
{ "config": {}, "data": {} }
```

### Why Execution State is Superior to Simple Completion Flags

| Aspect | Simple Flag | Execution State |
|--------|-------------|-----------------|
| Information richness | Binary only | Complete runtime state |
| Progress calculation | Impossible | Native support |
| Cross-engine consumption | Limited | Structured and queryable |
| Extensibility | Schema changes required | JSONB schema evolution |
| Debugging | Minimal context | Full state inspection |
| Future automation | Hard to extend | Natural for AI/automation |

## Alternatives Considered

### Alternative 1: Single Table with Nullable Execution-Specific Columns

Rejected because:
- Table bloat with many nullable columns
- Adding new execution types requires schema migrations
- No clear ownership of execution logic
- Difficult to validate type-specific constraints
- Violates Rule 30 (Engineering Over Speed) — premature complexity
- Hard to query across execution types

### Alternative 2: Single execution_type Column with Switch/Case Logic

Rejected because:
- Large switch/case statements throughout codebase
- Execution logic mixed with task lifecycle logic
- Difficult to test execution types in isolation
- Adding new types requires modifying existing code
- Violates Rule 27 (Preserve Architectural Consistency)
- High risk of conditional logic sprawl

### Alternative 3: Table-per-Execution-Type Inheritance

Rejected because:
- Too many tables for a single concept
- Complex joins for cross-type queries
- Difficult to maintain schema consistency
- Over-engineered for the problem
- Violates Rule 30 (Engineering Over Speed)
- Hard to add new execution types

### Alternative 4: Execution Provider Pattern with Strategy Objects (Selected)

Accepted because:
- Clean separation of execution logic from task lifecycle
- Each provider is independently testable
- Adding new execution types requires no changes to core Task Engine
- Providers encapsulate validation, progress, and completion rules
- JSONB execution state is flexible and queryable
- Aligns with Rule 42 (Task Execution Ownership)
- Supports Rule 32 (Engine Ownership) and Rule 35 (Engine Communication)

## Consequences

### Positive Consequences

- Task Engine remains the single owner of execution logic
- New execution types added without modifying core task code
- Execution providers are independently testable
- Execution State provides rich context for all consuming engines
- JSONB storage enables schema evolution without migrations
- Clean architecture with clear separation of concerns
- Supports future AI, XP, Memory, Analytics integrations
- No switch/case sprawl in task lifecycle code

### Negative Consequences

- More classes to maintain (one provider per execution type)
- JSONB validation requires custom logic
- Execution state migrations require careful handling
- Initial complexity higher than simple flag approach
- Requires discipline to keep providers focused

## Trade-offs

- **Gained**: Extensibility, clean architecture, testability, rich execution state
- **Gave up**: Simplicity of single flag, fewer initial classes
- **Assumption**: Execution types will grow over time as THE SYSTEM matures
- **Assumption**: JSONB validation overhead is acceptable for the flexibility gained
- **Assumption**: Execution providers will remain cohesive and not accumulate unrelated logic

## Future Impact

- All execution logic must go through Execution Providers
- Cross-engine events include structured execution state
- AI Engine can analyze execution patterns across types
- XP Engine can reward based on execution type and state
- Analytics Engine can compare execution patterns
- New execution types require: enum value, provider class, state schema, tests
- Execution State schema changes require backward-compatible JSONB migrations
- This pattern may be applied to other multi-mode engines (e.g., Habit Engine)

## Rule 42: Task Execution Ownership

Task execution behavior belongs exclusively to the Task Engine. Every execution type owns its own execution rules. Other engines consume execution results. No external engine may implement Task execution logic. Cross-engine communication occurs only through service interfaces or domain events.

## References

- Backend Constitution: Rule 27 (Preserve Architectural Consistency), Rule 30 (Engineering Over Speed), Rule 32 (Engine Ownership), Rule 35 (Engine Communication), Rule 36 (Independent Testability)
- Task Engine Design: `architecture/task-engine-design.md` (Section 7: Execution Provider Pattern, Section 8: Execution State)
- Goal Engine ADR: ADR-0006 (Cross-Engine Progress Calculation Pattern) — similar pattern applied to execution
- Strategy Pattern: Gang of Four, "Design Patterns: Elements of Reusable Object-Oriented Software"

---

*This ADR is part of THE SYSTEM Backend Architecture. All rules from the Backend Constitution apply.*
