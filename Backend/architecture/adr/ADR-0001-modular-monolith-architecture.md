# ADR-0001: Modular Monolith Architecture

## Status

Accepted

## Date

2026-08-07

## Authors

THE SYSTEM Architecture Team

## Context

THE SYSTEM is an AI-powered personal operating system expected to grow over multiple years with increasing complexity across multiple business domains: authentication, user profiles, goals, tasks, memory, AI interactions, gamification (XP), habits, sleep, health, analytics, notifications, calendar, integrations, and settings.

The architecture needed to support:
- Clear separation of concerns across business domains
- Independent development and testing of each domain
- Long-term maintainability as the codebase grows
- Potential future extraction of domains into separate services
- Team autonomy for different business capabilities

## Problem Statement

How should THE SYSTEM backend be organized to balance immediate development velocity with long-term scalability and maintainability?

The two primary architectural approaches considered were:
1. Traditional layered architecture (controllers, services, repositories, entities)
2. Microservices architecture from day one
3. Modular monolith architecture

## Decision

THE SYSTEM will use a **Modular Monolith** architecture.

The backend will be organized by business modules (engines) rather than technical layers. Each engine owns exactly one business capability and contains its own controllers, services, repositories, entities, DTOs, mappers, validators, exceptions, and events.

Engines communicate through well-defined service interfaces and domain events. No engine may directly access another engine's repositories or entities.

## Alternatives Considered

### Alternative 1: Traditional Layered Architecture

Rejected because:
- Does not enforce module boundaries
- Business logic spreads across technical layers
- Difficult to extract modules into services later
- No clear ownership of business capabilities
- Violates Rule 1 and Rule 2 of the Backend Constitution

### Alternative 2: Microservices from Day One

Rejected because:
- Excessive operational complexity for a new project
- No proven need for independent deployment
- Distributed system challenges (network latency, data consistency, monitoring)
- Higher development overhead for small teams
- Premature optimization contrary to Rule 30
- Violates the constitutional directive: "Do NOT implement microservices"

### Alternative 3: Modular Monolith (Selected)

Accepted because:
- Clear module boundaries with enforced ownership
- Single deployment artifact simplifies operations
- Easy to extract modules into services later if needed
- Supports independent testing and development
- Aligns with constitutional rules (Rule 1, Rule 2, Rule 32, Rule 33)
- Balances structure with pragmatism

## Consequences

### Positive Consequences

- Clear ownership boundaries for each business domain
- Engines can be developed and tested independently
- Easy to understand system structure
- Simple deployment (single artifact)
- Low operational overhead
- Natural migration path to microservices if needed
- Enforces dependency direction (no circular dependencies)
- Supports Rule 32 (Engine Ownership) and Rule 33 (One Source of Truth)

### Negative Consequences

- Requires discipline to maintain module boundaries
- All engines share the same database (requires careful schema management)
- Single deployment means all engines scale together
- Potential for module boundaries to erode over time without enforcement
- Requires explicit dependency management between engines

## Trade-offs

- **Gained**: Structural clarity, operational simplicity, development velocity
- **Gave up**: Independent scaling of individual engines, technology heterogeneity
- **Assumption**: Team discipline will maintain module boundaries
- **Assumption**: Future need for microservices is uncertain but path remains open

## Future Impact

- All subsequent engines must follow the modular structure defined in this ADR
- Database schema changes must respect engine ownership
- Cross-engine communication must use service interfaces or events
- Module extraction into services will be straightforward if requirements change
- Architecture documentation (module-constitution.md) must be maintained

## References

- Backend Constitution: Rule 1 (Module First Architecture), Rule 2 (Module Ownership), Rule 32 (Engine Ownership), Rule 33 (One Source of Truth), Rule 34 (No Circular Dependencies), Rule 35 (Engine Communication), Rule 36 (Independent Testability)
- Architecture Document: `architecture/module-constitution.md`
- [Modular Monolith Pattern](https://en.wikipedia.org/wiki/Modular_monolith)

---

*This ADR is part of THE SYSTEM Backend Architecture. All rules from the Backend Constitution apply.*
