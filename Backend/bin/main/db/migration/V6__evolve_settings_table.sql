-- =====================================================
-- V6: Evolve settings table for Settings Engine
-- =====================================================

-- Make user_id nullable for system-wide settings
ALTER TABLE settings ALTER COLUMN user_id DROP NOT NULL;

-- Add new columns
ALTER TABLE settings ADD COLUMN IF NOT EXISTS namespace VARCHAR(100) NOT NULL DEFAULT 'default';
ALTER TABLE settings ADD COLUMN IF NOT EXISTS value_type VARCHAR(20) NOT NULL DEFAULT 'STRING';
ALTER TABLE settings ADD COLUMN IF NOT EXISTS value_json JSONB NULL;
ALTER TABLE settings ADD COLUMN IF NOT EXISTS description TEXT NULL;
ALTER TABLE settings ADD COLUMN IF NOT EXISTS is_system BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill namespace from existing key values (default namespace)
UPDATE settings SET namespace = 'default' WHERE namespace IS NULL;

-- Drop old unique constraint
DROP INDEX IF EXISTS uq_settings_user_id_key;

-- Create new unique constraints for user settings (user_id IS NOT NULL)
CREATE UNIQUE INDEX IF NOT EXISTS uq_settings_user_namespace_key
    ON settings(user_id, namespace, key)
    WHERE user_id IS NOT NULL AND deleted_at IS NULL;

-- Create new unique constraints for system settings (user_id IS NULL)
CREATE UNIQUE INDEX IF NOT EXISTS uq_settings_system_namespace_key
    ON settings(namespace, key)
    WHERE user_id IS NULL AND deleted_at IS NULL;

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_settings_user_id ON settings(user_id);
CREATE INDEX IF NOT EXISTS idx_settings_namespace ON settings(namespace);
CREATE INDEX IF NOT EXISTS idx_settings_deleted_at ON settings(deleted_at);
CREATE INDEX IF NOT EXISTS idx_settings_is_system ON settings(is_system);
CREATE INDEX IF NOT EXISTS idx_settings_created_at ON settings(created_at);
