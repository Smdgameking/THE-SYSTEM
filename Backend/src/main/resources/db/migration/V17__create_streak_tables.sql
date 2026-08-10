-- ============================================================================
-- V17: Create streak tables
-- ============================================================================
-- Creates the persistence foundation for the unified global user streak.
-- This migration is intentionally limited to schema and does not contain
-- any streak calculation logic, services, controllers, or event listeners.

-- ---------------------------------------------------------------
-- user_streaks: materialized current streak state per user
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_streaks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    current_streak INTEGER NOT NULL DEFAULT 0,
    longest_streak INTEGER NOT NULL DEFAULT 0,
    current_streak_start_date DATE NULL,
    last_activity_date DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_user_streaks_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_current_streak_non_negative CHECK (current_streak >= 0),
    CONSTRAINT chk_longest_streak_non_negative CHECK (longest_streak >= 0),
    CONSTRAINT chk_longest_streak_gte_current CHECK (longest_streak >= current_streak)
);

-- Exactly one active streak row per user.
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_streaks_user_id_active
ON user_streaks(user_id)
WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_user_streaks_user_id
ON user_streaks(user_id);

CREATE INDEX IF NOT EXISTS idx_user_streaks_last_activity_date
ON user_streaks(last_activity_date);

-- ---------------------------------------------------------------
-- user_streak_history: event-level history for qualifying activity
-- ---------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_streak_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    activity_date DATE NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    source_engine VARCHAR(50) NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP NULL,
    CONSTRAINT fk_user_streak_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Prevent duplicate source events from creating duplicate history rows.
CREATE UNIQUE INDEX IF NOT EXISTS uq_user_streak_history_source
ON user_streak_history(source_engine, source_id, source_type)
WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_user_streak_history_user_id
ON user_streak_history(user_id);

CREATE INDEX IF NOT EXISTS idx_user_streak_history_user_activity_date
ON user_streak_history(user_id, activity_date);

CREATE INDEX IF NOT EXISTS idx_user_streak_history_activity_date
ON user_streak_history(activity_date);

CREATE INDEX IF NOT EXISTS idx_user_streak_history_source
ON user_streak_history(source_engine, source_id, source_type);
