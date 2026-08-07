# Roadmap

## Phase 1: Database Foundation (v0.2.0) - COMPLETED

- [x] Project initialization with Gradle Kotlin DSL
- [x] Java 21 toolchain configuration
- [x] PostgreSQL datasource configuration
- [x] Flyway migrations setup
- [x] BaseEntity with audit fields
- [x] Database migrations (V1-V3)
- [x] UUID primary keys
- [x] Snake_case naming
- [x] Soft delete support
- [x] Foreign keys and indexes

## Phase 2: Security Foundation (v0.3.0) - IN PROGRESS

- [x] JWT token provider
- [x] JWT authentication filter
- [x] BCrypt password encoder
- [x] Access/refresh token pattern
- [x] Security configuration
- [x] Auth module skeleton (entities, repositories, DTOs, mappers, services, controller)
- [x] Auth module unit tests
- [x] Auth module integration tests
- [ ] Auth module README documentation

## Phase 3: Core Modules (v0.4.0 - v0.6.0)

- [ ] User module (profile, preferences)
- [ ] Goal module (goals CRUD)
- [ ] Task module (tasks CRUD)
- [ ] Memory module (memories CRUD)
- [ ] AI module (AI interactions)

## Phase 4: Supporting Modules (v0.7.0 - v0.9.0)

- [ ] XP module (gamification)
- [ ] Habit module (habit tracking)
- [ ] Sleep module (sleep tracking)
- [ ] Health module (health metrics)
- [ ] Analytics module (reporting)
- [ ] Notification module (notifications)
- [ ] Calendar module (calendar integration)
- [ ] Integration module (third-party integrations)
- [ ] Settings module (user settings)

## Phase 5: Production Readiness (v1.0.0)

- [ ] Full test coverage (>80%)
- [ ] Performance testing
- [ ] Security audit
- [ ] Documentation complete
- [ ] Deployment automation
- [ ] Monitoring and alerting
- [ ] Backup and recovery
