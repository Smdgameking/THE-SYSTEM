# THE SYSTEM Backend Constitution

This document contains the permanent architectural rules for THE SYSTEM backend.

---

## Chapter 1: General Principles

### Rule 1: Module First Architecture

The backend is organized by business modules, not technical layers.

Correct:
```
modules/
    auth
    user
    goal
    task
    ...
```

Wrong:
```
controllers/
services/
repositories/
entities/
```

### Rule 2: Module Ownership

Each module owns exactly ONE business capability. No module should contain another module's business logic.

### Rule 3: Definition of Done

A module is NEVER complete until ALL of the following exist:
- Flyway Migration
- Entity
- Repository
- DTO
- Mapper
- Service Interface
- Service Implementation
- Controller
- Validation
- Exception Handling
- Security
- Unit Tests
- Integration Tests
- OpenAPI Documentation
- Logging
- README Documentation

### Rule 4: Controllers

Controllers receive HTTP requests, validate input, call services, and return responses. Controllers NEVER contain business logic.

### Rule 5: Services

Services own ALL business logic. Repositories never contain business logic. Controllers never contain business logic.

### Rule 6: Repositories

Repositories ONLY communicate with the database. Repositories never validate, calculate, authorize, or contain business rules.

### Rule 7: DTOs

Never expose JPA entities. REST APIs always return DTOs.

### Rule 8: Dependency Injection

Use Constructor Injection ONLY. Never use Field Injection.

### Rule 9: Database

Flyway is the ONLY schema management tool. Hibernate must never modify the schema.
Always use: `spring.jpa.hibernate.ddl-auto=validate`

### Rule 10: Primary Keys

Every business entity uses UUID. Never use auto-increment IDs unless explicitly approved.

### Rule 11: Audit

Every business entity contains: id, created_at, updated_at, created_by, updated_by, deleted_at.
Create a reusable BaseEntity. Every entity extends BaseEntity.

### Rule 12: Naming

- Database: snake_case
- Java: PascalCase Classes, camelCase Methods, UPPER_SNAKE_CASE Constants

### Rule 13: API Responses

Every API returns exactly the same response structure.

Success:
```json
{
  "success": true,
  "message": "...",
  "data": {},
  "timestamp": "...",
  "requestId": "..."
}
```

Failure:
```json
{
  "success": false,
  "error": {
      "code": "...",
      "message": "..."
  },
  "timestamp": "...",
  "requestId": "..."
}
```

### Rule 14: Security

Every endpoint is protected. Only explicitly marked endpoints are public.
Passwords: BCrypt
JWT: Access Token, Refresh Token
Never log passwords or tokens.

### Rule 15: Validation

Every incoming request is validated. Business logic must never receive invalid input.

### Rule 16: Logging

Log important operations. Never log passwords, JWTs, secrets, or sensitive personal data.

### Rule 17: Documentation

Every module contains documentation in `docs/`. Every README explains: Purpose, Database Tables, API Endpoints, Business Rules, Relationships, Events, Future Roadmap.

### Rule 18: Architecture Documents

Maintain architecture/ directory with:
- backend-constitution.md
- database-constitution.md
- api-constitution.md
- security-constitution.md
- module-constitution.md
- coding-standards.md
- decision-log.md
- roadmap.md

### Rule 19: Changelog

Every release updates CHANGELOG.md. Never skip changelog updates.

### Rule 20: Event Ready

Modules should communicate through interfaces. Future communication should support domain events. Avoid tight coupling.

### Rule 21: Testing

Every module requires Unit Tests and Integration Tests. A module without tests is incomplete.

### Rule 22: Code Quality

Before a module is marked COMPLETE:
- Build passes
- All tests pass
- No duplicated business logic
- No compiler warnings
- OpenAPI updated
- README updated

### Rule 23: Backward Compatibility

Breaking API changes require: Documentation, Migration Strategy, Version Increment.

### Rule 24: Think Long Term

Every design decision must answer: "Will this still make sense when THE SYSTEM has millions of users and years of development?" If NO, redesign it.

### Rule 25: No Skipping Phases

Never begin a new phase until the previous phase is completely finished.

### Rule 26: Standard Development Workflow

Every module must follow this order:
1. Architecture Design
2. Database Design
3. Flyway Migration
4. Entity
5. Repository
6. DTO
7. Mapper
8. Service Interface
9. Service Implementation
10. Controller
11. Validation
12. Security
13. Unit Tests
14. Integration Tests
15. OpenAPI Documentation
16. README Documentation
17. Build Verification
18. Module Completed

---

## Chapter 2: Engine Design

### Rule 27: Preserve Architectural Consistency

Consistent patterns are enforced across all modules: same package structure, same response format, same exception handling, same security approach. Architectural inconsistencies are treated as defects.

### Rule 28: No Business Logic in Kernel

Shared packages (`common`, `shared`, `security`, `config`) contain only infrastructure. No standalone Kernel contains business logic. All business logic resides in owning engines.

### Rule 29: Event-Driven Communication

Events are the primary mechanism for cross-engine communication. Engines publish events for significant state changes and consume events from other engines. Direct service-to-service calls are minimized.

### Rule 30: Engineering Over Speed

Every design decision must answer: "Will this still make sense when THE SYSTEM has millions of users and years of development?" If NO, redesign it. Avoid premature abstractions. Architecture must evolve from actual requirements, not speculative future needs.

### Rule 31: Single Source of Truth

Each engine owns its data exclusively. No duplicate data stores. Cross-engine data access through service interfaces or events only. Current state is derived from historical records, never by replacing them.

### Rule 32: Engine Ownership

Clear ownership boundaries are documented and enforced. Each engine owns exactly one business capability and its associated data. No engine may implement another engine's business logic.

### Rule 33: No Circular Dependencies

Engines must not form circular dependency chains. Communication flows through events and well-defined service interfaces. No engine imports another engine's implementation classes.

### Rule 34: Engine Communication

Service interfaces and domain events are the only allowed communication mechanisms between engines. Direct repository access, database joins across engine boundaries, and shared mutable state are prohibited.

### Rule 35: Independent Testability

Each engine must be testable in isolation. Engines have independent unit and integration tests. Mocks and test doubles are used to isolate the system under test. Tests run without external dependencies where possible.

### Rule 36: No Hard-Coded Secrets

Secrets, credentials, and configuration values must never be hard-coded in source code. Use environment variables, configuration files, or secret management systems. Development defaults are permitted only in non-production configurations.

### Rule 37: Immutable Entities

Entities must be treated as immutable data carriers. Prefer constructor initialization over setters. DTOs must use immutable types. Service layers construct entities. Entity state changes must go through service methods.

### Rule 38: Configuration Registration

All configuration definitions must be registered at startup. Unregistered configuration access is prohibited. Registration must occur before the application accepts requests.

### Rule 39: Configuration Definitions are Immutable

Once registered, configuration definitions cannot be modified, removed, or replaced. The registry locks after startup. Runtime modifications throw exceptions.

### Rule 40: Startup Registration Phase

Configuration registration is a mandatory startup phase. The application must not start serving requests until all definitions are registered and the registry is locked.

### Rule 41: Progressive Enhancement

Core functionality is implemented first. Advanced features are layered on top. Future integrations are documented but not prematurely implemented. The system must remain usable with only core features.

### Rule 42: Task Execution Ownership

Task execution behavior belongs exclusively to the Task Engine. Every execution type owns its own execution rules. Other engines consume execution results. No external engine may implement Task execution logic. Cross-engine communication occurs only through service interfaces or domain events.

### Rule 43: Architecture Compliance

Architecture is the source of truth. Implementation follows architecture. Implementation must never introduce undocumented architectural concepts. If implementation requires a new architectural concept:
1. Update architecture first.
2. If the concept affects multiple engines, create an ADR before implementation.

Implementation never leads architecture.

### Rule 44: Immutable Progress History

Long-term user progression must never overwrite history. Systems representing user progression, including XP, Achievements, and future progression systems, must preserve immutable historical records. Current state must be derived from historical records, not by replacing them.

### Rule 45: Idempotent Event Processing

Every engine consuming domain events must safely process duplicate events.

Duplicate delivery must never create duplicate business effects.

Consumers are responsible for idempotency. Producers are not responsible for guaranteeing exactly-once delivery.

Idempotency must be enforced using stable business identifiers.

### Rule 46: Constitution Governance

The Backend Constitution is the authoritative source for all project-wide engineering rules.

Every approved rule must exist in the Constitution.

Rules must not exist only in:
- ADRs
- Architecture documents
- Audit reports
- Conversations
- Implementation notes

Whenever a new constitutional rule is approved:
1. Add it to the Constitution.
2. Preserve continuous numbering.
3. Update the rule index if one exists.
4. Update references where necessary.

The Constitution is the canonical engineering policy for THE SYSTEM backend.

---

## Appendix: Rule Index

| Rule | Name |
|------|------|
| 1 | Module First Architecture |
| 2 | Module Ownership |
| 3 | Definition of Done |
| 4 | Controllers |
| 5 | Services |
| 6 | Repositories |
| 7 | DTOs |
| 8 | Dependency Injection |
| 9 | Database |
| 10 | Primary Keys |
| 11 | Audit |
| 12 | Naming |
| 13 | API Responses |
| 14 | Security |
| 15 | Validation |
| 16 | Logging |
| 17 | Documentation |
| 18 | Architecture Documents |
| 19 | Changelog |
| 20 | Event Ready |
| 21 | Testing |
| 22 | Code Quality |
| 23 | Backward Compatibility |
| 24 | Think Long Term |
| 25 | No Skipping Phases |
| 26 | Standard Development Workflow |
| 27 | Preserve Architectural Consistency |
| 28 | No Business Logic in Kernel |
| 29 | Event-Driven Communication |
| 30 | Engineering Over Speed |
| 31 | Single Source of Truth |
| 32 | Engine Ownership |
| 33 | No Circular Dependencies |
| 34 | Engine Communication |
| 35 | Independent Testability |
| 36 | No Hard-Coded Secrets |
| 37 | Immutable Entities |
| 38 | Configuration Registration |
| 39 | Configuration Definitions are Immutable |
| 40 | Startup Registration Phase |
| 41 | Progressive Enhancement |
| 42 | Task Execution Ownership |
| 43 | Architecture Compliance |
| 44 | Immutable Progress History |
| 45 | Idempotent Event Processing |
| 46 | Constitution Governance |

---

*This document is the authoritative source for all project-wide engineering rules. All rules from the Backend Constitution apply.*
