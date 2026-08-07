# THE SYSTEM Backend Constitution

This document contains the permanent architectural rules for THE SYSTEM backend.

## Project Philosophy

THE SYSTEM is an AI-powered Personal Operating System. Every design decision must prioritize:
- Maintainability
- Scalability
- Security
- Clean Architecture
- Consistency
- Long-term evolution

## Module First Architecture

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

## Module Ownership

Each module owns exactly ONE business capability. No module should contain another module's business logic.

## Definition of Done

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

## Controllers

Controllers receive HTTP requests, validate input, call services, and return responses. Controllers NEVER contain business logic.

## Services

Services own ALL business logic. Repositories never contain business logic. Controllers never contain business logic.

## Repositories

Repositories ONLY communicate with the database. Repositories never validate, calculate, authorize, or contain business rules.

## DTOs

Never expose JPA entities. REST APIs always return DTOs.

## Dependency Injection

Use Constructor Injection ONLY. Never use Field Injection.

## Database

Flyway is the ONLY schema management tool. Hibernate must never modify the schema.
Always use: `spring.jpa.hibernate.ddl-auto=validate`

## Primary Keys

Every business entity uses UUID. Never use auto-increment IDs unless explicitly approved.

## Audit

Every business entity contains: id, created_at, updated_at, created_by, updated_by, deleted_at.
Create a reusable BaseEntity. Every entity extends BaseEntity.

## Naming

- Database: snake_case
- Java: PascalCase Classes, camelCase Methods, UPPER_SNAKE_CASE Constants

## API Responses

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

## Security

Every endpoint is protected. Only explicitly marked endpoints are public.
Passwords: BCrypt
JWT: Access Token, Refresh Token
Never log passwords or tokens.

## Validation

Every incoming request is validated. Business logic must never receive invalid input.

## Logging

Log important operations. Never log passwords, JWTs, secrets, or sensitive personal data.

## Documentation

Every module contains documentation in `docs/`. Every README explains: Purpose, Database Tables, API Endpoints, Business Rules, Relationships, Events, Future Roadmap.

## Architecture Documents

Maintain architecture/ directory with:
- backend-constitution.md
- database-constitution.md
- api-constitution.md
- security-constitution.md
- module-constitution.md
- coding-standards.md
- decision-log.md
- roadmap.md

## Changelog

Every release updates CHANGELOG.md. Never skip changelog updates.

## Event Ready

Modules should communicate through interfaces. Future communication should support domain events. Avoid tight coupling.

## Testing

Every module requires Unit Tests and Integration Tests. A module without tests is incomplete.

## Code Quality

Before a module is marked COMPLETE:
- Build passes
- All tests pass
- No duplicated business logic
- No compiler warnings
- OpenAPI updated
- README updated

## Backward Compatibility

Breaking API changes require: Documentation, Migration Strategy, Version Increment.

## Think Long Term

Every design decision must answer: "Will this still make sense when THE SYSTEM has millions of users and years of development?" If NO, redesign it.

## No Skipping Phases

Never begin a new phase until the previous phase is completely finished.

## Standard Development Workflow

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
