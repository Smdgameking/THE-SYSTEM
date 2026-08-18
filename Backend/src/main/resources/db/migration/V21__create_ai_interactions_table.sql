-- ============================================================================
-- V21: Create ai_interactions table
-- ============================================================================
-- AI Engine Database Migration
-- Source: architecture/ai-engine-design.md section 4

CREATE TABLE IF NOT EXISTS ai_interactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    message TEXT NOT NULL,
    response TEXT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NULL,
    context JSONB NULL,
    prompt_tokens INTEGER NULL,
    completion_tokens INTEGER NULL,
    total_tokens INTEGER NULL,
    finish_reason VARCHAR(50) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_ai_interactions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================================
-- Indexes
-- ============================================================================

-- User-scoped access patterns
CREATE INDEX IF NOT EXISTS idx_ai_interactions_user_id ON ai_interactions(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_interactions_user_created ON ai_interactions(user_id, created_at) WHERE deleted_at IS NULL;

-- Soft delete
CREATE INDEX IF NOT EXISTS idx_ai_interactions_deleted_at ON ai_interactions(deleted_at);
