# Architecture Decision Records (ADRs)

This directory contains the Architecture Decision Records for THE SYSTEM backend.

ADRs are immutable historical documents that capture significant architectural decisions, their context, and their consequences.

## What is an ADR?

An Architecture Decision Record (ADR) is a short document that captures an important architectural decision made during the lifecycle of a project. ADRs serve as the long-term memory of the architecture team.

## Why ADRs Exist

- Preserve the rationale behind architectural decisions
- Enable future developers to understand why the system is built this way
- Provide a historical record of trade-offs considered
- Facilitate onboarding by documenting the "why" behind the architecture
- Support future evolution by making past decisions explicit
- Create accountability for architectural choices

## ADR Lifecycle

```
Proposed → Accepted → Superseded
                → Deprecated
                → Rejected
```

### Status Definitions

| Status | Description |
|--------|-------------|
| **Proposed** | Under active consideration. Not yet decided. |
| **Accepted** | Decision has been made and is being implemented. |
| **Superseded** | Replaced by a newer decision. Reference the new ADR. |
| **Deprecated** | No longer relevant but not replaced. |
| **Rejected** | Explicitly rejected with documented rationale. |

## Naming Convention

ADR files follow this naming pattern:

```
ADR-XXXX-short-title-in-kebab-case.md
```

Examples:
- `ADR-0001-modular-monolith-architecture.md`
- `ADR-0002-postgresql-as-primary-database.md`
- `ADR-0003-flyway-database-migrations.md`

## How to Create a New ADR

1. Copy `ADR-TEMPLATE.md` to a new file
2. Name it using the pattern `ADR-XXXX-description.md`
3. Assign the next available sequential number
4. Fill in all sections completely
5. Set initial status to `Proposed`
6. Submit for review
7. Once accepted, update status to `Accepted`
8. Never modify an accepted ADR — create a new ADR instead

## ADR Principles

- ADRs are immutable once accepted
- Never rewrite history — create new ADRs to supersede old ones
- Every significant decision deserves an ADR
- ADRs are not specifications — they record decisions
- Keep ADRs concise but complete
- Focus on "why" not "what"

## References

- [Documenting Architecture Decisions](https://thinkrelevance.com/blog/2011/11/15/documenting-architecture-decisions)
- [ADR GitHub Organization](https://adr.github.io/)

---

*This README is part of THE SYSTEM Backend Architecture. All rules from the Backend Constitution apply.*
