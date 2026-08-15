-- ============================================================================
-- V20: Create memories table
-- ============================================================================
-- Memory Engine Database Migration
-- Source: architecture/memory-engine-design.md section 4

CREATE TABLE IF NOT EXISTS memories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'NOTE',
    importance VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    source_id UUID NULL,
    tags JSONB NULL,
    custom_metadata JSONB NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_memories_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_memories_type CHECK (type IN ('NOTE', 'FACT', 'PREFERENCE', 'INSIGHT', 'SIGNAL')),
    CONSTRAINT chk_memories_importance CHECK (importance IN ('LOW', 'NORMAL', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_memories_source CHECK (source IN ('MANUAL', 'TASK', 'GOAL', 'AI'))
);

-- ============================================================================
-- Indexes
-- ============================================================================

-- User-scoped access patterns
CREATE INDEX IF NOT EXISTS idx_memories_user_id ON memories(user_id);
CREATE INDEX IF NOT EXISTS idx_memories_user_type ON memories(user_id, type) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_memories_user_importance ON memories(user_id, importance) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_memories_user_source ON memories(user_id, source) WHERE deleted_at IS NULL;

-- Cross-engine reference lookups (source_id is an opaque UUID, no FK)
CREATE INDEX IF NOT EXISTS idx_memories_source_id ON memories(source_id) WHERE deleted_at IS NULL AND source_id IS NOT NULL;

-- Soft delete
CREATE INDEX IF NOT EXISTS idx_memories_deleted_at ON memories(deleted_at);
