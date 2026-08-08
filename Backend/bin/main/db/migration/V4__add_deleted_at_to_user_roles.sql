-- =====================================================
-- V4: Add deleted_at to user_roles table
-- =====================================================
-- Constitution Rule 11: Every business entity must have deleted_at for soft deletion

ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS idx_user_roles_deleted_at ON user_roles(deleted_at);
