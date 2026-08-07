# Settings Module

## Purpose

The Settings Engine is the single source of truth for all configurable behavior within THE SYSTEM. It provides a unified, type-safe, validated configuration layer that serves both user preferences and application-wide settings.

## Responsibilities

### Owned by Settings Engine

- Setting Registry: Central registry of all configurable settings with mandatory metadata
- Setting Definitions: Metadata for every configurable setting (namespace, key, type, default, validator, description, visibility, owning engine)
- User Setting Values: Per-user overrides of default settings
- System Settings: Global application settings (not tied to a specific user)
- Validation: Type-specific validation for every setting value
- Default Management: Centralized default values with fallback logic
- History/Audit: Track setting changes over time
- Namespacing: Organize settings into logical groups
- Registration Enforcement: Prevent use of unregistered settings at runtime

### NOT Owned by Settings Engine

- Authentication and authorization logic
- User profile data
- Business rules of other engines
- How other engines interpret or use settings
- Feature flag rollout strategies (consumers handle this)

## Database

### settings table

- `id` UUID PRIMARY KEY
- `user_id` UUID NULL (FK to users, NULL for system settings)
- `namespace` VARCHAR(100) NOT NULL
- `key` VARCHAR(100) NOT NULL
- `value` TEXT NULL
- `value_json` JSONB NULL
- `value_type` VARCHAR(20) NOT NULL
- `description` TEXT NULL
- `is_system` BOOLEAN NOT NULL DEFAULT FALSE
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL
- `created_by` UUID
- `updated_by` UUID
- `deleted_at` TIMESTAMP NULL

### Indexes

- `uq_settings_user_namespace_key` on (user_id, namespace, key) WHERE deleted_at IS NULL AND user_id IS NOT NULL
- `uq_settings_system_namespace_key` on (namespace, key) WHERE deleted_at IS NULL AND user_id IS NULL
- `idx_settings_user_id` on (user_id)
- `idx_settings_namespace` on (namespace)
- `idx_settings_deleted_at` on (deleted_at)
- `idx_settings_is_system` on (is_system)

## Registry

Every setting must be registered before use. The registry is writable during startup and locked after all engines register their definitions.

### SettingDefinition

- namespace
- key
- type
- defaultValue
- description
- validator
- visibility
- owningEngine

### Visibility Levels

- PUBLIC: Visible to users in UI
- ENGINE: Internal engine setting
- ADMIN: Admin-only configuration
- PRIVATE: Sensitive setting, never exposed

## Value Types

- STRING
- BOOLEAN
- INTEGER
- DOUBLE
- JSON
- ENUM

## API Endpoints

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/settings/{namespace}/{key}` | Get single setting | User |
| PUT | `/settings/{namespace}/{key}` | Set single setting | User |
| DELETE | `/settings/{namespace}/{key}` | Delete setting | User |
| GET | `/settings/{namespace}` | Get namespace settings | User |
| PUT | `/settings/{namespace}` | Set namespace settings | User |
| POST | `/settings/{namespace}/reset` | Reset namespace | User |
| GET | `/settings/system/{namespace}/{key}` | Get system setting | Admin |
| PUT | `/settings/system/{namespace}/{key}` | Set system setting | Admin |
| GET | `/settings/definitions/{namespace}/{key}` | Get definition | User |
| GET | `/settings/definitions/namespace/{namespace}` | Get namespace definitions | User |
| GET | `/settings/definitions/engine/{engine}` | Get engine definitions | User |

## Business Rules

1. All settings must be registered before use
2. Definitions are immutable after startup
3. Users can only access their own settings
4. Admins can manage system settings
5. Validation occurs at registration and write time
6. Default values are resolved lazily
7. Registry locks after startup

## Events

- `SettingUpdatedEvent`
- `SettingCreatedEvent`
- `SettingDeletedEvent`
- `NamespaceUpdatedEvent`

## Future Expansion

- Setting history tracking
- Setting dependencies
- Conditional visibility
- Import/export user profiles
- A/B testing for defaults
- Remote configuration
