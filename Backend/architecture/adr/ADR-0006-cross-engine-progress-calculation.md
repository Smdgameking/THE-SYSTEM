# ADR-0006: Cross-Engine Progress Calculation Pattern

## Status

Proposed

## Date

2026-08-07

## Authors

THE SYSTEM Architecture Team

## Context

THE SYSTEM's Goal Engine must calculate goal progress based on contributions from multiple engines (Task Engine, XP Engine, Milestones). Different goals require different completion strategies: task-based, milestone-based, XP-based, manual, or custom.

The challenge is to enable the Goal Engine to calculate progress without:
- Tight coupling to specific engine implementations
- Blocking the Goal Engine while waiting for external calculations
- Creating circular dependencies between engines
- Duplicating business logic across engines

## Problem Statement

How can THE SYSTEM enable the Goal Engine to calculate progress based on contributions from other engines while maintaining strict module boundaries and avoiding circular dependencies?

Options considered:
1. Direct service calls from Goal Engine to other engines
2. Shared progress calculation service in Kernel
3. Event-driven progress aggregation with cached results
4. Hybrid strategy pattern with engine-specific calculators

## Decision

**Event-driven progress aggregation with strategy-pattern calculators** is the selected approach.

### Core Principles

1. **Goal Engine owns progress logic**: The Goal Engine determines HOW to calculate progress for each strategy type
2. **Other engines publish events**: Task Engine publishes `TaskCompletedEvent`, XP Engine publishes `XPAwardedEvent`
3. **Goal Engine subscribes to relevant events**: Subscribes only to events that affect progress
4. **Progress is recalculated asynchronously**: Events trigger recalculation, not synchronous blocking
5. **Strategy pattern isolates calculation logic**: Each completion strategy is encapsulated

### Architecture

```
┌─────────────────┐     publishes      ┌──────────────────┐
│   Task Engine   │───────────────────▶│                  │
│                 │  TaskCompletedEvent │   Goal Engine    │
└─────────────────┘                     │                  │
                                        │  - Owns progress │
┌─────────────────┐     publishes      │  - Subscribes to │
│    XP Engine    │───────────────────▶│    events        │
│                 │  XPAwardedEvent     │  - Applies       │
└─────────────────┘                     │    strategy      │
                                        │  - Publishes     │
                                        │    GoalProgress  │
                                        │    UpdatedEvent  │
                                        └──────────────────┘
```

### Strategy Pattern

```java
public interface ProgressCalculator {
    double calculateProgress(Goal goal, ProgressContext context);
    boolean isComplete(Goal goal, ProgressContext context);
}

public class TaskBasedCalculator implements ProgressCalculator {
    public double calculateProgress(Goal goal, ProgressContext context) {
        // Query Task Engine via service interface
        // NOT direct repository access
    }
}
```

### Event Flow

1. Task Engine completes task
2. Task Engine publishes `TaskCompletedEvent`
3. Goal Engine receives event asynchronously
4. Goal Engine identifies affected goals
5. Goal Engine recalculates progress using appropriate strategy
6. Goal Engine updates goal in database
7. Goal Engine publishes `GoalProgressUpdatedEvent`
8. Other engines (XP, Analytics, Notification) react to progress update

## Alternatives Considered

### Alternative 1: Direct Service Calls

Rejected because:
- Goal Engine must know about Task Engine, XP Engine APIs
- Creates tight coupling between engines
- Violates Rule 35 (Engine Communication through interfaces only)
- Synchronous calls block goal operations
- Difficult to test in isolation

### Alternative 2: Shared Progress Service in Kernel

Rejected because:
- Violates Kernel Evolution Principle (Kernel should not contain business logic)
- Progress calculation is business logic, not infrastructure
- Creates central bottleneck
- Difficult to extend with new strategies

### Alternative 3: Event-Driven with Synchronous Calculation

Rejected because:
- Event handlers run synchronously in many configurations
- Blocks the publishing engine
- Complex error handling and retry logic
- Difficult to guarantee consistency

### Alternative 4: Event-Driven with Strategy Pattern (Selected)

Accepted because:
- Goal Engine owns progress calculation logic
- Other engines remain decoupled (publish events only)
- Strategy pattern enables extensible completion strategies
- Asynchronous processing prevents blocking
- Clear ownership: Goal Engine = progress authority
- Supports Rule 32 (Engine Ownership) and Rule 35 (Engine Communication)

## Consequences

### Positive Consequences

- Loose coupling between engines via events
- Goal Engine maintains single source of truth for progress
- Strategy pattern enables new completion types without engine changes
- Asynchronous processing improves responsiveness
- Clear ownership and testing boundaries
- Extensible for future engines

### Negative Consequences

- Eventual consistency (progress not updated instantly)
- More complex than direct calls
- Requires reliable event delivery mechanism
- Debugging distributed progress updates is harder
- Requires careful handling of out-of-order events

## Trade-offs

- **Gained**: Decoupling, extensibility, engine autonomy, async performance
- **Gave up**: Immediate consistency, simplicity of direct calls
- **Assumption**: Event infrastructure will be implemented reliably
- **Assumption**: Slight delay in progress updates is acceptable to users

## Future Impact

- All engines that contribute to goal progress must publish events
- Progress calculation strategies can be added without modifying other engines
- Event schema becomes part of the public API contract
- Future engines (Analytics, AI) can contribute to progress via events
- This pattern may be applied to other cross-engine calculations

## References

- Backend Constitution: Rule 32 (Engine Ownership), Rule 33 (One Source of Truth), Rule 34 (No Circular Dependencies), Rule 35 (Engine Communication), Rule 36 (Independent Testability)
- Goal Engine Design: `architecture/goal-engine-design.md` (Section 10: Progress System, Section 13: Engine Communication)
- Kernel Evolution Principle: Events are infrastructure, progress calculation is business logic
- Settings Engine: Similar pattern for settings validation

---

*This ADR is part of THE SYSTEM Backend Architecture. All rules from the Backend Constitution apply.*
