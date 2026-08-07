# ADR-0005: Kernel Evolution Philosophy

## Status

Accepted

## Date

2026-08-07

## Authors

THE SYSTEM Architecture Team

## Context

THE SYSTEM is designed as a long-lived, evolving system. The architecture needs a mechanism to share common infrastructure across engines without creating tight coupling or premature abstraction.

The question arose: Should a "Kernel" be created upfront to house shared utilities, or should shared infrastructure emerge naturally as needs are proven across multiple engines?

## Problem Statement

How should THE SYSTEM handle shared infrastructure that is used by multiple engines?

Options considered:
1. Create a Kernel upfront with all anticipated shared components
2. No Kernel — each engine owns its utilities
3. Evolve Kernel naturally from repeated cross-engine needs (selected)

## Decision

**THE SYSTEM does not have a standalone Kernel implementation today.**

The Kernel is an architectural vision that must evolve naturally as the project grows. Only infrastructure required by multiple engines may be promoted into the Kernel.

**Principles:**
- The Kernel provides shared platform capabilities, not business functionality
- Business logic must NEVER belong in the Kernel
- The Kernel is discovered through evolution, not speculation
- Never build Kernel components in anticipation of future needs
- Only extract common infrastructure after repeated usage across multiple engines has proven the need

**Current shared infrastructure locations:**
- `com.thesystem.common` — Common utilities, constants, exceptions, responses
- `com.thesystem.shared` — Shared entities (BaseEntity), events (DomainEvent)
- `com.thesystem.security` — Security infrastructure (JWT, passwords, filters)
- `com.thesystem.config` — Application configuration classes

## Alternatives Considered

### Alternative 1: Create Kernel Upfront

Rejected because:
- Violates "Never build Kernel components in anticipation of future needs"
- Creates unused abstractions that constrain design
- Adds complexity without proven benefit
- Difficult to determine what truly belongs in Kernel
- Violates Rule 30 (Engineering Over Speed) — premature abstraction

### Alternative 2: No Shared Infrastructure

Rejected because:
- Duplication of common code across engines
- Inconsistent implementations of shared concerns
- No central place for cross-cutting concerns (security, logging, etc.)
- Violates DRY principle and Rule 27 (Preserve Architectural Consistency)

### Alternative 3: Evolve Kernel Naturally (Selected)

Accepted because:
- Aligns with constitutional directive: "The Kernel is discovered through evolution, not speculation"
- Prevents premature abstraction
- Keeps business logic in owning engines
- Shared infrastructure emerges from actual needs
- Clear criteria for promotion to Kernel: repeated usage across multiple engines
- Maintains module boundaries while enabling code reuse

## Consequences

### Positive Consequences

- No speculative abstractions in the codebase
- Shared infrastructure is proven by real usage
- Clear ownership: engines own business logic, Kernel owns only infrastructure
- Easy to understand what belongs where
- Prevents "utility graveyard" of unused code
- Maintains modular monolith boundaries

### Negative Consequences

- Some duplication may occur before promotion to Kernel
- Requires discipline to refactor when patterns emerge
- No upfront guidance on what should be shared
- Team must recognize duplication and propose promotion

## Trade-offs

- **Gained**: Practicality, evolutionary design, reduced premature complexity
- **Gave up**: Upfront architectural purity, predictable shared structure
- **Assumption**: Team will recognize duplication and propose Kernel promotion
- **Assumption**: The set of truly shared infrastructure is smaller than anticipated

## Future Impact

- As engines grow, common patterns will emerge
- When a utility is used by 3+ engines, consider Kernel promotion
- Kernel candidates: Event Bus, Scheduler, Request Context, Security Context, Audit Pipeline, Logging Infrastructure, Metrics, Health Monitoring, Feature Flags, Engine Registry, Shared Configuration
- Kernel components must never contain business logic
- Promotion to Kernel requires ADR and constitutional approval

## References

- Backend Constitution: Kernel Evolution Principle, Rule 27 (Preserve Architectural Consistency), Rule 30 (Engineering Over Speed), Rule 35 (Engine Communication)
- Architecture Document: `architecture/backend-constitution.md` (Kernel Evolution Principle section)
- Current shared packages: `com.thesystem.common`, `com.thesystem.shared`, `com.thesystem.security`, `com.thesystem.config`

---

*This ADR is part of THE SYSTEM Backend Architecture. All rules from the Backend Constitution apply.*
