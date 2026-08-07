# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.0] - 2026-08-07

### Added
- Project initialization with Gradle Kotlin DSL
- Java 21 LTS toolchain configuration
- Spring Boot 3.4.5 application framework
- PostgreSQL 17 database configuration
- Flyway database migration tool
- SpringDoc OpenAPI documentation
- Spring Boot Actuator for monitoring
- Lombok and MapStruct for code generation
- Testcontainers and H2 for testing

### Database
- V1__database_initialization.sql: Enable pgcrypto extension
- V2__create_auth_tables.sql: users, roles, user_roles tables
- V3__create_settings_table.sql: settings table
- UUID primary keys for all tables
- Audit columns (created_at, updated_at, created_by, updated_by, deleted_at)
- Foreign key constraints and indexes
- Snake_case naming convention

### Architecture
- Modular monolith package structure
- 15 business module packages with sub-packages
- BaseEntity with audit fields
- Standardized ApiResponse format
- DomainEvent base class for event-driven architecture
- RequestIdGenerator for request tracking

### Security Foundation
- JWT token provider (access/refresh tokens)
- JWT authentication filter
- BCrypt password encoder (cost factor 12)
- Security configuration with stateless JWT
- Password hashing and validation service

### Configuration
- application.yml (default profile)
- application-dev.yml (development)
- application-prod.yml (production)
- logback-spring.xml (logging configuration)
- Dockerfile and docker-compose.yml

### Documentation
- Architecture documents in architecture/ directory
- Backend constitution
- Database constitution
- API constitution
- Security constitution
- Module constitution
- Coding standards
- Decision log
- Roadmap

### Quality
- All tests passing
- Build verification successful
- Production-quality code standards

## [0.6.0] - 2026-08-07

### Added
- Goal Engine module (objective management)
- V7__create_goals_and_milestones.sql: goals and goal_milestones tables
- Goal enums: GoalStatus, GoalPriority, GoalDifficulty, GoalType, GoalVisibility, CompletionStrategy
- Goal entity with all approved fields (title, description, category, priority, difficulty, status, visibility, estimated_xp, progress, completion_percentage, target_date, completion_strategy, tags JSONB, custom_metadata JSONB)
- GoalMilestone entity with display_order, is_completed, completed_date
- GoalRepository and GoalMilestoneRepository with optimized queries
- Goal DTOs: GoalResponse, GoalDetailResponse, CreateGoalRequest, UpdateGoalRequest, MilestoneResponse, CreateMilestoneRequest, UpdateMilestoneRequest, GoalStatisticsResponse
- GoalMapper (MapStruct) with custom JSON/list conversions
- GoalService interface and implementation with full lifecycle management
- GoalController with 20 endpoints covering CRUD, lifecycle, milestones, statistics
- GoalExceptionHandler
- Domain events: GoalCreatedEvent, GoalUpdatedEvent, GoalStartedEvent, GoalPausedEvent, GoalCompletedEvent, GoalArchivedEvent, GoalDeletedEvent, GoalProgressUpdatedEvent
- State machine for goal lifecycle transitions (DRAFT → ACTIVE → PAUSED/COMPLETED/FAILED → ARCHIVED)
- Completion strategy pattern (MANUAL, TASK_BASED, XP_BASED, MILESTONE_BASED, PERCENTAGE, CUSTOM)
- Milestone CRUD with display_order management
- Goal statistics endpoint
- Goal module unit tests (8 test cases)
- Goal module integration tests (7 test cases)
- docs/goal/README.md

### Database
- V7 migration creates goals and goal_milestones tables
- UUID primary keys, audit fields, soft delete
- JSONB columns for tags and custom_metadata
- Indexes on user_id, status, priority, category, target_date
- Unique constraint on goal_milestones (goal_id, display_order)
- Foreign keys with ON DELETE CASCADE

### Security
- Users manage only their own goals
- Reuses existing JWT authentication
- No duplicated authentication logic

## [0.5.1] - 2026-08-07

### Added
- Settings Engine module (configuration management)
- V6__evolve_settings_table.sql: Evolved settings table with namespace, key, value_type, value_json, description, is_system
- SettingType enum (STRING, BOOLEAN, INTEGER, DOUBLE, JSON, ENUM)
- Visibility enum (PUBLIC, ENGINE, ADMIN, PRIVATE)
- SettingDefinition record with validator, visibility, owningEngine
- InMemorySettingRegistry with startup registration and lock mechanism
- Setting entity with JSONB support
- SettingRepository with namespace and system queries
- Setting DTOs: SettingResponse, NamespaceSettingsResponse, SetSettingRequest, SettingDefinitionResponse
- SettingMapper (MapStruct)
- SettingsService interface and implementation
- SettingsController with 11 endpoints:
  - GET /settings/{namespace}/{key}
  - PUT /settings/{namespace}/{key}
  - DELETE /settings/{namespace}/{key}
  - GET /settings/{namespace}
  - PUT /settings/{namespace}
  - POST /settings/{namespace}/reset
  - GET /settings/system/{namespace}/{key}
  - PUT /settings/system/{namespace}/{key}
  - GET /settings/definitions/{namespace}/{key}
  - GET /settings/definitions/namespace/{namespace}
  - GET /settings/definitions/engine/{engine}
- SettingsExceptionHandler
- Configuration Registry with immutability enforcement
- Registry lock after startup
- Type-safe value conversion and validation
- UserDetailsServiceImpl for role-based authorization
- SecurityUtils.isAdmin() helper
- Settings module unit tests (6 test cases)
- Settings module integration tests (8 test cases)
- docs/settings/README.md

### Database
- V6 migration evolves settings table
- Added namespace, key, value_type, value_json, description, is_system columns
- Unique constraints for user and system settings
- JSONB support for complex configuration values
- Partial indexes for user/system setting separation

### Security
- Users can manage only their own settings
- Admins can manage system settings
- Registry locked after startup prevents runtime definition changes

## [0.4.0] - 2026-08-07

### Added
- User Engine module (profile management)
- V5__create_user_profiles_table.sql: user_profiles table
- User entity and repository
- UserProfile DTOs: UserProfileResponse, UpdateProfileRequest, PublicUserResponse
- UserProfileMapper (MapStruct)
- UserService interface and implementation
- UserController with 4 endpoints:
  - GET /users/me
  - PUT /users/me
  - GET /users/{id}
  - GET /users/username/{username}
- UserExceptionHandler
- SecurityUtils for current user extraction
- User module unit tests (6 test cases)
- User module integration tests (6 test cases)
- docs/user/README.md

### Database
- V5 migration creates user_profiles table with UUID PK, FK to users
- Username uniqueness constraint
- Audit fields on user_profiles
- Soft delete support
- Indexes on user_id, username, deleted_at, created_at

### Security
- All user endpoints require authentication
- Users can only update their own profile
- Reuses existing JWT authentication

## [0.3.0] - 2026-08-07

### Added
- Security foundation
- JWT token provider with access/refresh tokens
- JWT authentication filter
- BCrypt password encoder (cost factor 12)
- Auth module complete with all Definition of Done items
- Auth module unit and integration tests
- docs/auth/README.md

## [0.1.0] - 2026-08-07

### Added
- Initial project structure
- Package organization
- Configuration classes
- Build configuration
- Docker setup
