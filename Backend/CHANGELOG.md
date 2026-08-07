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

## [0.1.0] - 2026-08-07

### Added
- Initial project structure
- Package organization
- Configuration classes
- Build configuration
- Docker setup
