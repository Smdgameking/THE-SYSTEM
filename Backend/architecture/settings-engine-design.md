# Settings Engine Design Document

## Version: 1.0.0
## Date: 2026-08-07
## Status: Draft - Pending Approval

---

## 1. Purpose

The Settings Engine is the single source of truth for all configurable behavior within THE SYSTEM. It provides a unified, type-safe, validated configuration layer that serves both user preferences and application-wide settings.

The Settings Engine eliminates scattered configuration logic, inconsistent defaults, and duplicated validation across engines.

---

## 2. Responsibilities

### Owned by Settings Engine

- **Setting Registry**: Central registry of all configurable settings with mandatory metadata
- **Setting Definitions**: Metadata for every configurable setting (namespace, key, type, default, validator, description, visibility, owning engine)
- **User Setting Values**: Per-user overrides of default settings
- **System Settings**: Global application settings (not tied to a specific user)
- **Validation**: Type-specific validation for every setting value
- **Default Management**: Centralized default values with fallback logic
- **History/Audit**: Track setting changes over time (optional, deferred to post-v1)
- **Namespacing**: Organize settings into logical groups (notification, ai, calendar, etc.)
- **Registration Enforcement**: Prevent use of unregistered settings at runtime

### NOT Owned by Settings Engine

- Authentication and authorization logic
- User profile data
- Business rules of other engines
- How other engines interpret or use settings
- Feature flag rollout strategies (consumers handle this)

---

## 3. Database Design

### 3.1 Existing Settings Table Assessment

The existing `settings` table (V3 migration) contains:
- `id` (UUID PK)
- `user_id` (UUID, FK to users)
- `key` (VARCHAR)
- `value` (TEXT)
- Audit fields

**Decision: EVOLVE the existing table.**

Rationale:
- Rule 28: Never remove database objects unless explicitly instructed
- Existing table has correct FK relationship to `users`
- Structure is compatible with evolution
- Preserves any existing data and dependencies

### 3.2 Recommended Schema

```sql
CREATE TABLE IF NOT EXISTS settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NULL,  -- NULL for system-wide settings
    namespace VARCHAR(100) NOT NULL,
    key VARCHAR(100) NOT NULL,
    value TEXT NULL,           -- For STRING, BOOLEAN, INTEGER, DOUBLE, ENUM
    value_json JSONB NULL,     -- For JSON type
    value_type VARCHAR(20) NOT NULL,  -- STRING, BOOLEAN, INTEGER, DOUBLE, JSON, ENUM
    description TEXT NULL,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP NULL
);

-- Unique constraint: one setting per (user_id, namespace, key)
-- For system settings (user_id IS NULL), one per (namespace, key)
CREATE UNIQUE INDEX IF NOT EXISTS uq_settings_user_namespace_key 
    ON settings(user_id, namespace, key) 
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_settings_system_namespace_key 
    ON settings(namespace, key) 
    WHERE user_id IS NULL AND deleted_at IS NULL;

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_settings_user_id ON settings(user_id);
CREATE INDEX IF NOT EXISTS idx_settings_namespace ON settings(namespace);
CREATE INDEX IF NOT EXISTS idx_settings_deleted_at ON settings(deleted_at);
CREATE INDEX IF NOT EXISTS idx_settings_is_system ON settings(is_system);

-- Foreign key
ALTER TABLE settings 
    ADD CONSTRAINT fk_settings_user 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
```

### 3.3 Schema Evolution Strategy

1. **Backward Compatibility**: Existing `settings` rows are migrated to new schema
   - `namespace` defaults to `"default"`
   - `key` preserved
   - `value` preserved
   - `value_type` inferred or set to `STRING`
   - `is_system` set to `FALSE`

2. **No Downtime**: Add columns as nullable first, backfill, then set NOT NULL

3. **Data Migration**: Application-level migration of existing rows

### 3.4 Entity Relationships

```
users (1) ─── (N) settings
    │
    └── user_id FK with ON DELETE CASCADE
```

---

## 4. Setting Registry

### 4.1 Registration Requirement

Every configurable setting MUST be registered before it can be used. No engine may create or consume unregistered settings.

The Settings Engine maintains the authoritative registry of all settings.

### 4.2 SettingDefinition Structure

```java
public record SettingDefinition(
    String namespace,           // e.g., "notification", "ai", "appearance"
    String key,                 // e.g., "email_enabled", "theme"
    SettingType type,           // STRING, BOOLEAN, INTEGER, DOUBLE, JSON, ENUM
    Object defaultValue,        // Default value matching the type
    String description,         // Human-readable description
    Validator<?> validator,     // Validation function
    Visibility visibility,      // PUBLIC, ENGINE, ADMIN, PRIVATE
    String owningEngine         // e.g., "notification", "ai", "app"
) {}
```

### 4.3 Visibility Levels

| Visibility | Description | Accessible By |
|-----------|-------------|---------------|
| PUBLIC | Visible to users in UI | All users |
| ENGINE | Internal engine setting | Owning engine only |
| ADMIN | Admin-only configuration | Administrators only |
| PRIVATE | Sensitive setting, never exposed | System only |

### 4.4 Registration API

```java
public interface SettingRegistry {
    
    // Register a new setting definition
    void register(SettingDefinition definition);
    
    // Register multiple definitions at once
    void registerAll(List<SettingDefinition> definitions);
    
    // Get definition by namespace and key
    SettingDefinition getDefinition(String namespace, String key);
    
    // Get all definitions in a namespace
    List<SettingDefinition> getDefinitionsByNamespace(String namespace);
    
    // Get all definitions owned by an engine
    List<SettingDefinition> getDefinitionsByOwningEngine(String engine);
    
    // Check if setting is registered
    boolean isRegistered(String namespace, String key);
    
    // Validate that a value conforms to definition
    void validate(String namespace, String key, Object value);
}
```

### 4.5 Startup Registration Phase

**Every engine must register its configuration definitions during application startup.**

The Configuration Registry operates in two distinct phases:

#### Phase 1: Registration (Startup)
- **Application Startup**: All engines register their settings during initialization
- **Engine Initialization**: Each engine registers settings when its context loads
- **Registry State**: WRITABLE
- **Allowed Operations**: `register`, `registerAll`
- **Validation**: Duplicate detection, type consistency, required fields
- **Order**: Engines register in dependency order (no circular dependencies)

#### Phase 2: Locked (Runtime)
- **Trigger**: After all engines have completed registration
- **Registry State**: LOCKED
- **Allowed Operations**: Read-only (`getDefinition`, `isRegistered`, `validate`)
- **Forbidden Operations**: Any modification (`register`, `registerAll`) throws `BusinessException`
- **Duration**: For the entire application runtime

```java
// Registry lifecycle
public interface SettingRegistry {
    // Phase 1 only: Registration
    void register(SettingDefinition definition);      // Throws if locked
    void registerAll(List<SettingDefinition> definitions); // Throws if locked
    
    // Phase 1 and Phase 2: Read-only
    SettingDefinition getDefinition(String namespace, String key);
    List<SettingDefinition> getDefinitionsByNamespace(String namespace);
    List<SettingDefinition> getDefinitionsByOwningEngine(String engine);
    boolean isRegistered(String namespace, String key);
    void validate(String namespace, String key, Object value);
    
    // Lifecycle management
    boolean isLocked();
    void lock(); // Called automatically after startup
}
```

### 4.6 Lock Mechanism

- **Automatic Lock**: Registry locks automatically after Spring context refresh
- **Manual Lock**: Can be triggered manually for testing
- **Lock Verification**: Every write operation checks `isLocked()` before proceeding
- **Lock Persistence**: Lock state is maintained in memory; no database persistence needed
- **Restart Required**: Any definition changes require application restart

```java
// Lock enforcement example
public void register(SettingDefinition definition) {
    if (this.locked) {
        throw new BusinessException(
            ErrorCodes.CONFLICT, 
            "Setting registry is locked. Definitions cannot be modified after startup."
        );
    }
    // ... registration logic
}
```

### 4.7 Definition Immutability

**Configuration Definitions are immutable after application startup.**

- Definitions describe the architecture and are part of the application code
- Only configuration **values** may change during execution
- Changing a definition requires a code change and deployment
- In production, the registry is read-only after initialization
- Any attempt to modify or re-register a definition at runtime must throw `BusinessException`
- The `SettingRegistry` interface exposes only read-only operations in production

```java
// Production registry - read-only after startup
public interface SettingRegistry {
    SettingDefinition getDefinition(String namespace, String key);
    List<SettingDefinition> getDefinitionsByNamespace(String namespace);
    List<SettingDefinition> getDefinitionsByOwningEngine(String engine);
    boolean isRegistered(String namespace, String key);
    void validate(String namespace, String key, Object value);
}
```

### 4.8 Enforcement

```java
// Throws BusinessException if setting is not registered
public <T> T getSetting(UUID userId, String namespace, String key, Class<T> type);

// Throws BusinessException if setting is not registered
public void setSetting(UUID userId, String namespace, String key, Object value);
```

No engine may bypass the registry. Direct database access to `settings` table is prohibited by architecture rules and enforced via code review.

Definitions and values are strictly separated:
- **Definitions**: Code, immutable, deployed with application
- **Values**: Data, mutable, stored in database, changed at runtime

---

## 5. Supported Value Types

| Type | Storage Column | Java Type | Validation |
|------|---------------|-----------|------------|
| STRING | `value` | `String` | Min/max length, pattern |
| BOOLEAN | `value` | `Boolean` | Must be "true" or "false" |
| INTEGER | `value` | `Long` | Min/max range |
| DOUBLE | `value` | `Double` | Min/max range, precision |
| JSON | `value_json` | `JsonNode` | Schema validation (optional) |
| ENUM | `value` | `String` | Must be in allowed values list |

### 5.1 Type Coercion Rules

- **Read**: Convert stored value to requested type. Throw `BusinessException` if incompatible.
- **Write**: Store as string in `value` or JSON in `value_json`. Validate against type constraints.
- **Default**: Each setting definition specifies its type. Type mismatch with default is a configuration error.

---

## 6. Validation Strategy

### 6.1 Setting-Level Validation

Every setting definition includes:
```java
public record SettingDefinition(
    String namespace,
    String key,
    SettingType type,
    Object defaultValue,
    String description,
    Validator<?> validator,
    Visibility visibility,
    String owningEngine
)
```

### 6.2 Validator Types

| Validator | Applies To | Rule |
|-----------|-----------|------|
| NotNull | ALL | Value cannot be null if required |
| Min/Max | INTEGER, DOUBLE | Numeric range |
| MinLength/MaxLength | STRING | String length |
| Pattern | STRING | Regex match |
| AllowedValues | ENUM | Must be in predefined list |
| JsonSchema | JSON | Must match JSON schema |
| Custom | ANY | Engine-specific validation |

### 6.3 Validation Timing

- **On Registration**: Validate definition completeness, uniqueness, type consistency
- **On Set**: Validate value against definition before saving
- **On Get**: Return default if user value is invalid (log warning)
- **On Migration**: Validate all existing values during schema migration

---

## 7. Default Management

### 7.1 Default Hierarchy

1. **System Default**: Defined in code by Settings Engine
2. **Engine Default**: Override suggested by owning engine
3. **User Value**: Explicitly set by user

Resolution order: User Value → Engine Default → System Default

### 7.2 Default Storage

Defaults are NOT stored in the database by default. They are defined in code:

```java
public enum SystemSetting {
    THEME("appearance", "theme", SettingType.STRING, "light"),
    NOTIFICATIONS_ENABLED("notification", "enabled", SettingType.BOOLEAN, true),
    DEFAULT_TIMEZONE("appearance", "timezone", SettingType.STRING, "UTC");
    
    // ...
}
```

### 7.3 Lazy Initialization

User-specific defaults are created lazily on first access:
1. User requests setting
2. Check if user override exists
3. If not, return system default
4. Optionally create user row for tracking

---

## 8. Caching Strategy

### 8.1 Cache Layers

| Layer | Technology | Scope | TTL |
|-------|-----------|-------|-----|
| L1: In-Process | Caffeine | Per-request / Thread | Request-scoped |
| L2: Application | Spring Cache + Caffeine | Per-user, Per-namespace | 5 minutes |
| L3: Distributed | Redis (future) | Shared across instances | 5 minutes |

### 8.2 Cache Keys

```
settings:{userId}:{namespace}    → Map<String, Object>
settings:system:{namespace}      → Map<String, Object>
```

### 8.3 Invalidation

- **Write-through**: Cache updated on every write
- **Immediate Invalidation**: On setting update, remove from cache
- **Batch Invalidation**: On bulk update, invalidate entire namespace
- **TTL Expiry**: Safety net for distributed cache

### 8.4 Cache Warming

- On user login, pre-load frequently accessed namespaces
- On application startup, load system settings into cache

---

## 9. Inter-Engine Communication

### 9.1 Service Interface Exposure

Other engines access settings ONLY through `SettingsService`:

```java
public interface SettingsService {
    <T> T getSetting(UUID userId, String namespace, String key, Class<T> type);
    <T> T getSettingOrDefault(UUID userId, String namespace, String key, Class<T> type, T defaultValue);
    void setSetting(UUID userId, String namespace, String key, Object value);
    Map<String, Object> getNamespaceSettings(UUID userId, String namespace);
    boolean exists(UUID userId, String namespace, String key);
}
```

### 9.2 Domain Events

Settings Engine publishes:
- `SettingUpdatedEvent` — When a user setting changes
- `SettingCreatedEvent` — When a new setting is created
- `SettingDeletedEvent` — When a setting is deleted
- `NamespaceUpdatedEvent` — When multiple settings in a namespace change

Other engines subscribe to events they care about:
```java
@Component
public class NotificationSettingSubscriber {
    @EventListener
    public void onSettingUpdated(SettingUpdatedEvent event) {
        if ("notification".equals(event.namespace())) {
            // Refresh notification preferences
        }
    }
}
```

### 9.3 What Engines Should NOT Do

- Directly query `settings` table
- Bypass `SettingsService` for setting values
- Assume settings exist without handling defaults
- Cache settings independently (use Settings Engine cache)

---

## 10. Public API Design

### 10.1 Service Interface

```java
public interface SettingsService {
    
    // Get a single setting
    <T> T getSetting(UUID userId, String namespace, String key, Class<T> type);
    
    // Get with fallback
    <T> T getSettingOrDefault(UUID userId, String namespace, String key, Class<T> type, T defaultValue);
    
    // Get all settings in a namespace
    Map<String, Object> getNamespaceSettings(UUID userId, String namespace);
    
    // Get all settings for a user
    Map<String, Map<String, Object>> getAllUserSettings(UUID userId);
    
    // Set a setting (validates before saving)
    void setSetting(UUID userId, String namespace, String key, Object value);
    
    // Set multiple settings in a namespace
    void setNamespaceSettings(UUID userId, String namespace, Map<String, Object> values);
    
    // Check if setting exists
    boolean exists(UUID userId, String namespace, String key);
    
    // Delete a setting (revert to default)
    void deleteSetting(UUID userId, String namespace, String key);
    
    // Reset namespace to defaults
    void resetNamespaceToDefaults(UUID userId, String namespace);
    
    // Get system settings (no user context)
    <T> T getSystemSetting(String namespace, String key, Class<T> type);
    
    // Set system setting (admin only)
    void setSystemSetting(String namespace, String key, Object value);
}
```

### 10.2 REST API Design

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/settings/{namespace}/{key}` | Get single setting | User |
| GET | `/settings/{namespace}` | Get all settings in namespace | User |
| PUT | `/settings/{namespace}/{key}` | Set single setting | User |
| PUT | `/settings/{namespace}` | Set multiple settings | User |
| DELETE | `/settings/{namespace}/{key}` | Delete setting (revert) | User |
| POST | `/settings/{namespace}/reset` | Reset namespace to defaults | User |
| GET | `/system/settings/{namespace}/{key}` | Get system setting | Admin |
| PUT | `/system/settings/{namespace}/{key}` | Set system setting | Admin |

**Note**: REST endpoints are designed only. Not implemented in this phase.

### 10.3 DTOs

```java
// Request
public record SetSettingRequest(
    Object value,
    SettingType type  // Optional, inferred if not provided
) {}

// Response
public record SettingResponse(
    String namespace,
    String key,
    Object value,
    SettingType type,
    String description,
    boolean isSystem,
    Instant updatedAt
) {}

// Namespace summary
public record NamespaceSettingsResponse(
    String namespace,
    Map<String, SettingResponse> settings
) {}
```

---

## 11. Scalability Analysis

### 11.1 Small Scale (10 settings per user)

- Single database query per namespace
- No special optimization needed
- In-memory cache sufficient

### 11.2 Medium Scale (100 settings per user)

- Namespace-based queries efficient with index on `(user_id, namespace, key)`
- Cache hit ratio > 90%
- No pagination needed for individual user

### 11.3 Large Scale (1000+ settings per user)

- **Database**: Index on `(user_id, namespace, key)` handles lookups efficiently
- **Namespacing**: Logical grouping prevents full-table scans
- **Pagination**: API supports pagination for namespace listing
- **Caching**: Essential - L2 cache per namespace
- **Architecture**: Consider namespace-based sharding if needed
- **No Redesign Needed**: Current schema supports unlimited growth

### 11.4 Growth Projections

| Users | Settings/User | Total Rows | Query Time (indexed) |
|-------|--------------|------------|---------------------|
| 10K | 100 | 1M | < 5ms |
| 100K | 100 | 10M | < 10ms |
| 1M | 100 | 100M | < 20ms |
| 10M | 1000 | 10B | Requires sharding (future) |

---

## 12. Authorization Model

### 12.1 Access Rules

| Operation | User Context | Admin Context |
|-----------|-------------|---------------|
| Read own settings | Allowed | Allowed |
| Write own settings | Allowed | Allowed |
| Read system settings | Denied | Allowed |
| Write system settings | Denied | Allowed |
| Read other user's settings | Denied | Allowed (audit) |

### 12.2 Implementation

- `@PreAuthorize` on service methods
- `userId` extracted from JWT for user-scoped operations
- `hasRole('ADMIN')` for system settings
- Never expose sensitive settings (passwords, tokens) via API

---

## 13. Engine Ownership

### 13.1 Settings Engine Owns

| Component | Owner |
|-----------|-------|
| Setting definitions (metadata) | Settings Engine |
| Setting registry | Settings Engine |
| User setting values | Settings Engine |
| System setting values | Settings Engine |
| Validation logic | Settings Engine |
| Default value management | Settings Engine |
| Namespace organization | Settings Engine |
| Setting history/audit | Settings Engine |
| Cache invalidation | Settings Engine |

### 13.2 Other Engines Own

| Component | Owner |
|-----------|-------|
| How to interpret a setting | Consuming Engine |
| Business rules around settings | Consuming Engine |
| UI for setting configuration | Frontend |
| Feature flag rollout | Consuming Engine |

### 13.3 Prohibited Actions

- Other engines MUST NOT directly query `settings` table
- Other engines MUST NOT modify setting values without calling `SettingsService`
- Other engines MUST NOT assume setting existence without handling defaults
- Settings Engine MUST NOT contain business logic of other engines
- Other engines MUST NOT use settings without registration (Rule 38)

---

## 14. Future Expansion

### 14.1 Planned Enhancements

1. **Setting History**: Track changes with `setting_history` table
2. **Setting Dependencies**: Setting A requires Setting B
3. **Conditional Visibility**: Show/hide settings based on other settings
4. **Import/Export**: User setting profiles
5. **A/B Testing**: Different defaults for cohorts
6. **Remote Configuration**: Dynamic setting updates without deploy

### 14.2 Extensibility Points

- Pluggable validators via `Validator` interface
- Custom type converters via `SettingConverter` interface
- Event subscribers for reactive updates
- Namespace-based permissions
- Visibility-based access control

---

## 15. Risks and Trade-offs

### 15.1 Risks

| Risk | Mitigation |
|------|-----------|
| Schema migration breaks existing data | Careful backfill strategy, test migration |
| Cache inconsistency | Write-through pattern, short TTL |
| Performance with 1000+ settings per user | Namespacing, indexing, caching |
| Circular dependencies with consuming engines | Strict Rule 34 enforcement, service-only access |
| Validation complexity | Centralized validators, type-safe API |
| Unregistered setting usage | Runtime enforcement, startup validation, code review |

### 15.2 Trade-offs

| Decision | Trade-off |
|----------|-----------|
| Evolve existing table vs. new table | Evolve: less disruptive, but legacy constraints |
| TEXT for all values vs. typed columns | TEXT + type column: flexible, but requires casting |
| Lazy defaults vs. eager creation | Lazy: less DB writes, but extra query on first access |
| In-memory cache vs. always-DB | Cache: faster, but eventual consistency |
| Generic API vs. typed endpoints | Generic: flexible, but less IDE support |
| Strict registration vs. dynamic creation | Strict: safer, but more upfront work |

---

## 16. Implementation Phases

### Phase 1 (v0.5.0) - Foundation

- Evolve `settings` table schema
- `SettingDefinition` registry
- Basic CRUD operations
- Type coercion
- Validation framework
- In-memory caching
- Registration enforcement

### Phase 2 (v0.6.0) - Integration

- Domain events
- Namespace management
- System settings
- Admin REST endpoints
- Visibility-based access control

### Phase 3 (v1.0.0) - Production

- Redis distributed cache
- Setting history
- Import/export
- Advanced validators
- Performance optimization

---

## 17. Open Questions

1. Should setting history be enabled by default or opt-in?
2. Should system settings be stored in the same table or separate?
3. Should we support hierarchical namespaces (e.g., `notification.email.sound`)?
4. What is the maximum allowed setting value size?
5. Should settings be versioned?
6. Should registration be allowed in production for emergency settings?
7. How do we handle setting deprecation?

---

## 18. Approval

- [ ] Lead Architect
- [ ] Security Engineer
- [ ] Database Administrator
- [ ] Product Owner

---

*This document is part of THE SYSTEM Backend Architecture. All rules from the Backend Constitution apply.*
