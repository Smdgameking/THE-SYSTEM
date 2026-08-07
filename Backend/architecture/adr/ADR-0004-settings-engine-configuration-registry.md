# ADR-0004: Settings Engine Configuration Registry

## Status

Accepted

## Date

2026-08-07

## Authors

THE SYSTEM Architecture Team

## Context

THE SYSTEM requires a unified, type-safe, validated configuration layer for both user preferences and system-wide settings. Multiple engines (notification, AI, calendar, etc.) need to consume and potentially define settings.

Without a central registry:
- Settings are scattered across engines with inconsistent validation
- Duplicate settings may emerge across namespaces
- No enforcement of setting definitions before use
- Type safety is lost when storing settings as generic strings
- Defaults are inconsistent or duplicated
- No visibility into what settings exist or who owns them

The Settings Engine needed a mechanism to enforce that every configurable setting is registered, validated, and immutable after definition.

## Problem Statement

How can THE SYSTEM ensure that all configuration is properly defined, validated, and governed without allowing runtime drift or inconsistent implementation across engines?

Options considered:
1. No registry — engines define settings ad-hoc
2. Database-only registry — settings defined only in database
3. Code-only registry — settings defined only in code
4. Hybrid registry with startup lock (selected)

## Decision

A **Settings Engine Configuration Registry** was implemented with the following characteristics:

- **SettingDefinition** record captures: namespace, key, type, defaultValue, description, validator, visibility, owningEngine
- **InMemorySettingRegistry** stores definitions in memory
- **Startup Registration Phase**: All engines register definitions during application startup
- **Registry Lock**: After startup, the registry locks permanently
- **Runtime Enforcement**: Access to unregistered settings throws `BusinessException`
- **Immutability**: Definitions cannot be added, removed, or modified after lock (Rule 39, Rule 40)

## Alternatives Considered

### Alternative 1: No Registry

Rejected because:
- Violates Rule 38 (Configuration Registration)
- No enforcement of setting definitions
- Duplicate settings across engines
- No validation before use
- Type safety impossible to guarantee

### Alternative 2: Database-Only Registry

Rejected because:
- Settings definitions become mutable at runtime
- Violates Rule 39 (Definitions are immutable)
- Database becomes source of truth instead of code
- Deployment can change architecture without code review
- No compile-time safety

### Alternative 3: Code-Only Without Lock

Rejected because:
- No enforcement at runtime
- Risk of late registration during development
- No guarantee all settings are registered before use
- Violates Rule 40 (Startup Registration Phase)

### Alternative 4: Hybrid Registry with Lock (Selected)

Accepted because:
- Definitions are code-based (immutable, version-controlled)
- Registry validates all definitions at registration time
- Lock prevents runtime modification
- Unregistered access fails fast with clear error
- Supports Rule 38, Rule 39, and Rule 40
- Enables type-safe value coercion
- Centralizes validation logic

## Consequences

### Positive Consequences

- Single source of truth for all settings
- Type-safe setting access with compile-time definition checking
- Validation occurs at registration and write time
- Prevents undefined or inconsistent settings
- Enables audit of all configurable behavior
- Supports visibility levels (PUBLIC, ENGINE, ADMIN, PRIVATE)
- Registry lock ensures architectural integrity in production
- Clear ownership via `owningEngine` field

### Negative Consequences

- All engines must register settings during startup
- Requires discipline to maintain registration discipline
- In-memory registry requires restart to add new settings
- Additional boilerplate for each new setting
- Lock mechanism must be reliable and tested

## Trade-offs

- **Gained**: Architectural integrity, type safety, validation, immutability
- **Gave up**: Runtime flexibility for definitions, dynamic setting creation
- **Assumption**: All settings are known at application design time
- **Assumption**: Restart is acceptable for adding new settings

## Future Impact

- All future engines must register their settings via the registry
- Registry implementation may evolve (distributed cache, persistent store)
- Setting definitions become part of the application contract
- Adding new settings requires code change and deployment
- Registry lock must be tested for thread safety and startup ordering

## References

- Backend Constitution: Rule 38 (Configuration Registration), Rule 39 (Configuration Definitions are Immutable), Rule 40 (Startup Registration Phase)
- Architecture Document: `architecture/settings-engine-design.md`
- Implementation: `com.thesystem.modules.settings.registry` package
- Settings Engine: Phase 3 (v0.5.1) implementation

---

*This ADR is part of THE SYSTEM Backend Architecture. All rules from the Backend Constitution apply.*
