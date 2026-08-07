# ADR-0003: Flyway Database Migrations

## Status

Accepted

## Date

2026-08-07

## Authors

THE SYSTEM Architecture Team

## Context

THE SYSTEM requires a reliable, version-controlled approach to database schema management. The schema must evolve safely across development, staging, and production environments without data loss or manual intervention.

Requirements:
- Version control for every schema change
- Repeatable migrations across environments
- Rollback capability (or at least forward-only safety)
- Team collaboration on schema changes
- Production safety (no accidental modifications)
- Audit trail of schema evolution

## Problem Statement

How should database schema changes be managed throughout the lifecycle of THE SYSTEM?

Options considered:
1. Flyway versioned migrations
2. Liquibase
3. Hibernate auto-DDL
4. Manual SQL scripts

## Decision

**Flyway** is selected as the sole database schema management tool.

Configuration:
- Versioned migrations: `V{version}__{description}.sql`
- Baseline on migrate: enabled
- Locations: `classpath:db/migration`
- Hibernate: `ddl-auto=validate` only

## Alternatives Considered

### Alternative 1: Liquibase

Rejected because:
- XML/YAML/JSON change logs are more verbose than SQL
- Larger runtime footprint
- Spring Boot integration is slightly less seamless than Flyway
- Team preference for plain SQL migrations
- Flyway's simplicity aligns better with project philosophy

### Alternative 2: Hibernate Auto-DDL

Rejected because:
- Violates Rule 9: "Hibernate must never modify the schema"
- No version control for schema changes
- Non-deterministic across environments
- Dangerous in production (accidental schema modifications)
- No audit trail of schema evolution
- Cannot handle complex migrations (data migration, index creation order)

### Alternative 3: Manual SQL Scripts

Rejected because:
- No automated version tracking
- Easy to miss migrations in deployment
- No rollback or repair mechanism
- No baseline management
- No validation against current schema state

### Alternative 4: Flyway (Selected)

Accepted because:
- Simple, focused, opinionated approach
- Versioned migrations with clear naming
- Strong Spring Boot integration
- Automatic validation and repair
- Repeatable across all environments
- SQL-first approach (no DSL learning curve)
- Industry standard for Spring Boot applications
- Supports baseline, undo (if needed), and repeatable migrations
- Enforces `ddl-auto=validate` philosophy

## Consequences

### Positive Consequences

- Single source of truth for schema evolution
- Migrations are version-controlled and reviewed
- Safe deployment process (validate before apply)
- Reproducible across all environments
- Clear audit trail in `flyway_schema_history`
- Prevents accidental schema modifications
- Enforces discipline in schema design
- Supports database refactoring best practices

### Negative Consequences

- Requires manual migration writing (no auto-generation)
- Developers must learn Flyway conventions
- Merge conflicts in migration files require manual resolution
- Cannot automatically generate migrations from entity changes
- Requires discipline to never modify applied migrations

## Trade-offs

- **Gained**: Schema safety, version control, reproducibility, audit trail
- **Gave up**: Auto-generation convenience, flexibility of manual scripts
- **Assumption**: Team will follow migration-first discipline
- **Assumption**: Schema changes are infrequent enough to warrant manual review

## Future Impact

- All schema changes must be implemented as Flyway migrations
- Hibernate `ddl-auto` will remain `validate` permanently
- Migration files are immutable once applied to production
- Complex migrations requiring data transformation must be handled in application code or multi-step migrations
- Database rollback strategy must be designed per migration (Flyway does not auto-rollback)

## References

- Backend Constitution: Rule 9 (Database), Rule 25 (No Skipping Phases)
- Configuration: `application.yml` (`spring.flyway.enabled=true`, `ddl-auto=validate`)
- Migrations: `src/main/resources/db/migration/`
- [Flyway Documentation](https://flywaydb.org/documentation/)

---

*This ADR is part of THE SYSTEM Backend Architecture. All rules from the Backend Constitution apply.*
