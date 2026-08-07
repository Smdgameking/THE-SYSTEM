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

## Phase 2: Security Foundation (v0.3.0) - COMPLETED

- [x] JWT token provider
- [x] JWT authentication filter
- [x] BCrypt password encoder
- [x] Access/refresh token pattern
- [x] Security configuration
- [x] Auth module skeleton (entities, repositories, DTOs, mappers, services, controller)
- [x] Auth module unit tests
- [x] Auth module integration tests
- [x] Auth module README documentation

## Phase 3: User Engine (v0.4.0) - COMPLETED

- [x] User module (profile, preferences)
- [x] V5 migration for user_profiles table
- [x] UserProfile entity and repository
- [x] UserProfile DTOs and mapper
- [x] UserService interface and implementation
- [x] UserController with 4 endpoints
- [x] User module unit tests
- [x] User module integration tests
- [x] docs/user/README.md

## Phase 4: Core Modules (v0.5.0 - v0.7.0)

- [ ] Goal module (goals CRUD)
- [ ] Task module (tasks CRUD)
- [ ] Memory module (memories CRUD)
- [ ] AI module (AI interactions)

## Phase 5: Supporting Modules (v0.8.0 - v1.0.0)

- [ ] XP module (gamification)
- [ ] Habit module (habit tracking)
- [ ] Sleep module (sleep tracking)
- [ ] Health module (health metrics)
- [ ] Analytics module (reporting)
- [ ] Notification module (notifications)
- [ ] Calendar module (calendar integration)
- [ ] Integration module (third-party integrations)
- [ ] Settings module (user settings)

## Phase 6: Production Readiness (v1.0.0)

- [ ] Full test coverage (>80%)
- [ ] Performance testing
- [ ] Security audit
- [ ] Documentation complete
- [ ] Deployment automation
- [ ] Monitoring and alerting
- [ ] Backup and recovery
