-- =====================================================
-- V7: Create goals and goal_milestones tables
-- =====================================================

CREATE TABLE IF NOT EXISTS goals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    difficulty VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    estimated_xp INTEGER NOT NULL DEFAULT 0,
    current_progress INTEGER NOT NULL DEFAULT 0,
    completion_percentage DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    target_date TIMESTAMP NULL,
    completed_date TIMESTAMP NULL,
    archived_date TIMESTAMP NULL,
    completion_strategy VARCHAR(50),
    tags JSONB NULL,
    custom_metadata JSONB NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_goals_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS goal_milestones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    display_order INTEGER NOT NULL DEFAULT 0,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_date TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_goal_milestones_goal FOREIGN KEY (goal_id) REFERENCES goals(id) ON DELETE CASCADE
);

-- Goals indexes
CREATE INDEX IF NOT EXISTS idx_goals_user_id ON goals(user_id);
CREATE INDEX IF NOT EXISTS idx_goals_user_status ON goals(user_id, status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_goals_status ON goals(status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_goals_priority ON goals(priority) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_goals_category ON goals(category) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_goals_target_date ON goals(target_date) WHERE deleted_at IS NULL AND target_date IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_goals_deleted_at ON goals(deleted_at);
CREATE INDEX IF NOT EXISTS idx_goals_created_at ON goals(created_at);

-- Milestones indexes
CREATE INDEX IF NOT EXISTS idx_goal_milestones_goal_id ON goal_milestones(goal_id);
CREATE INDEX IF NOT EXISTS idx_goal_milestones_display_order ON goal_milestones(goal_id, display_order);
CREATE INDEX IF NOT EXISTS idx_goal_milestones_deleted_at ON goal_milestones(deleted_at);

-- Unique constraint for milestone ordering per goal
CREATE UNIQUE INDEX IF NOT EXISTS uq_goal_milestones_goal_display_order
    ON goal_milestones(goal_id, display_order)
    WHERE deleted_at IS NULL;
