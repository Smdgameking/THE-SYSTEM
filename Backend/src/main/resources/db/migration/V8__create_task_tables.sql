-- V8__create_task_tables.sql
-- Task Engine Database Migration
-- Source: architecture/task-engine-design.md sections 4.1 through 4.5

-- ============================================================================
-- 4.1 Tasks Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    goal_id UUID NULL,
    parent_task_id UUID NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    category VARCHAR(50),
    execution_type VARCHAR(20) NOT NULL DEFAULT 'BOOLEAN',
    estimated_duration INTEGER NULL,
    actual_duration INTEGER NULL,
    start_date TIMESTAMP NULL,
    due_date TIMESTAMP NULL,
    completed_date TIMESTAMP NULL,
    reminder_date TIMESTAMP NULL,
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    recurring_config_id UUID NULL,
    tags JSONB NULL,
    attachments JSONB NULL,
    notes TEXT,
    completion_evidence JSONB NULL,
    execution_state JSONB NULL,
    custom_metadata JSONB NULL,
    visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_tasks_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_goal FOREIGN KEY (goal_id) REFERENCES goals(id) ON DELETE SET NULL,
    CONSTRAINT fk_tasks_parent FOREIGN KEY (parent_task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

-- ============================================================================
-- 4.2 Task Dependencies Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS task_dependencies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL,
    depends_on_task_id UUID NOT NULL,
    dependency_type VARCHAR(20) NOT NULL DEFAULT 'BLOCKS',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    resolved_date TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_task_dependencies_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_dependencies_depends_on FOREIGN KEY (depends_on_task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT uq_task_dependency UNIQUE (task_id, depends_on_task_id)
);

-- ============================================================================
-- 4.3 Task Time Entries Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS task_time_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL,
    user_id UUID NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NULL,
    duration_minutes INTEGER NULL,
    entry_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_task_time_entries_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_task_time_entries_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================================
-- 4.4 Recurring Task Configs Table
-- ============================================================================
CREATE TABLE IF NOT EXISTS recurring_task_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    interval_value INTEGER NOT NULL DEFAULT 1,
    cron_expression VARCHAR(100) NULL,
    days_of_week JSONB NULL,
    day_of_month INTEGER NULL,
    month INTEGER NULL,
    exception_dates JSONB NULL,
    end_date TIMESTAMP NULL,
    max_occurrences INTEGER NULL,
    occurrence_count INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    CONSTRAINT fk_recurring_task_configs_task FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
);

-- ============================================================================
-- 4.5 Indexes
-- ============================================================================

-- User tasks
CREATE INDEX IF NOT EXISTS idx_tasks_user_id ON tasks(user_id);
CREATE INDEX IF NOT EXISTS idx_tasks_user_status ON tasks(user_id, status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_user_due_date ON tasks(user_id, due_date) WHERE deleted_at IS NULL AND due_date IS NOT NULL;

-- Filtering
CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_priority ON tasks(priority) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_category ON tasks(category) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_due_date ON tasks(due_date) WHERE deleted_at IS NULL AND due_date IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_goal_id ON tasks(goal_id) WHERE deleted_at IS NULL AND goal_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_parent_task_id ON tasks(parent_task_id) WHERE deleted_at IS NULL AND parent_task_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_is_recurring ON tasks(is_recurring) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_tasks_execution_type ON tasks(execution_type) WHERE deleted_at IS NULL;

-- Dependencies
CREATE INDEX IF NOT EXISTS idx_task_dependencies_task_id ON task_dependencies(task_id);
CREATE INDEX IF NOT EXISTS idx_task_dependencies_depends_on ON task_dependencies(depends_on_task_id);

-- Time entries
CREATE INDEX IF NOT EXISTS idx_task_time_entries_task_id ON task_time_entries(task_id);
CREATE INDEX IF NOT EXISTS idx_task_time_entries_user_id ON task_time_entries(user_id);

-- Recurring configs
CREATE INDEX IF NOT EXISTS idx_recurring_task_configs_task_id ON recurring_task_configs(task_id);
CREATE INDEX IF NOT EXISTS idx_recurring_task_configs_is_active ON recurring_task_configs(is_active) WHERE is_active = TRUE;

-- Soft delete
CREATE INDEX IF NOT EXISTS idx_tasks_deleted_at ON tasks(deleted_at);
CREATE INDEX IF NOT EXISTS idx_task_dependencies_deleted_at ON task_dependencies(deleted_at);
CREATE INDEX IF NOT EXISTS idx_task_time_entries_deleted_at ON task_time_entries(deleted_at);
