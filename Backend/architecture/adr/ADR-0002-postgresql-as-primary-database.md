# ADR-0002: PostgreSQL as Primary Database

## Status

Accepted

## Date

2026-08-07

## Authors

THE SYSTEM Architecture Team

## Context

THE SYSTEM requires a primary relational database to store:
- User accounts and authentication data
- User profiles and preferences
- Business entities (goals, tasks, memories, etc.)
- Configuration settings
- Audit trails and soft-deleted records

The database needed to support:
- Strong consistency for transactional data
- Complex queries and joins across related entities
- JSON storage for flexible configuration values
- Full-text search capabilities (future)
- UUID primary keys for distributed-friendly identifiers
- Mature ecosystem and operational tooling

## Problem Statement

Which database system should serve as the primary data store for THE SYSTEM backend?

Options considered:
1. PostgreSQL
2. MySQL/MariaDB
3. MongoDB
4. SQL Server

## Decision

**PostgreSQL** is selected as the primary database for THE SYSTEM.

Version: PostgreSQL 17+ (or latest stable)
Driver: PostgreSQL JDBC Driver
ORM: Spring Data JPA with Hibernate
Migration Tool: Flyway

## Alternatives Considered

### Alternative 1: MySQL/MariaDB

Rejected because:
- Inferior JSON support compared to PostgreSQL JSONB
- Less robust full-text search capabilities
- Weaker standards compliance
- Fewer advanced data types (no native UUID, no arrays)
- Community momentum has shifted toward PostgreSQL

### Alternative 2: MongoDB

Rejected because:
- Document model does not fit relational business data
- Weak support for complex joins and transactions
- Eventual consistency conflicts with Rule 9 (Flyway-managed schema)
- No native support for ACID transactions across multiple documents
- Schema validation requires application-level enforcement
- Loses the benefits of relational data integrity

### Alternative 3: SQL Server

Rejected because:
- Licensing costs and vendor lock-in
- Smaller open-source ecosystem
- Less common in cloud-native deployments
- PostgreSQL offers equivalent or superior features

### Alternative 4: PostgreSQL (Selected)

Accepted because:
- Excellent JSONB support for flexible data (Settings Engine)
- Native UUID data type
- Strong ACID compliance
- Mature full-text search (future use)
- Open-source with permissive license
- Excellent Spring Boot integration
- Strong operational tooling (pgcrypto, pg_stat_statements)
- Industry standard for modern Java applications
- Supports concurrent indexing and partitioning for scalability

## Consequences

### Positive Consequences

- Robust data integrity with ACID transactions
- Native JSONB for Settings Engine and future flexible data
- UUID primary keys supported natively
- Strong ecosystem and community support
- Excellent PostgreSQL-specific features (pgcrypto for UUID generation)
- Proven at massive scale (Instagram, Spotify, etc.)
- Spring Data JPA/Hibernate optimized for PostgreSQL

### Negative Consequences

- Requires PostgreSQL-specific knowledge for operations
- JSONB flexibility can lead to schema drift if misused
- Connection pooling configuration requires tuning
- Backup/restore procedures specific to PostgreSQL

## Trade-offs

- **Gained**: Data integrity, JSONB flexibility, native UUID, ecosystem maturity
- **Gave up**: Schema simplicity of NoSQL, vendor neutrality
- **Assumption**: PostgreSQL will remain the dominant open-source RDBMS
- **Assumption**: Operational team has PostgreSQL expertise or can acquire it

## Future Impact

- All database objects use PostgreSQL-specific features (UUID, JSONB, pgcrypto)
- Flyway migrations target PostgreSQL dialect
- Connection pool configured for PostgreSQL (HikariCP)
- Future sharding/partitioning strategies will use PostgreSQL features
- Database migration to another RDBMS would require significant effort

## References

- Backend Constitution: Rule 9 (Database), Rule 10 (Primary Keys), Rule 11 (Audit), Rule 12 (Naming)
- Configuration: `application.yml`, `application-dev.yml`, `application-prod.yml`
- Migration: `V1__database_initialization.sql` (enables pgcrypto)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

---

*This ADR is part of THE SYSTEM Backend Architecture. All rules from the Backend Constitution apply.*
