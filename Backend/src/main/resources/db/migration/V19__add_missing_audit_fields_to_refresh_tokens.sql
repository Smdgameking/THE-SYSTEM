-- ============================================================================
-- V19: Add missing BaseEntity audit columns to refresh_tokens
-- ============================================================================

ALTER TABLE refresh_tokens ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE refresh_tokens ADD COLUMN created_by UUID;
ALTER TABLE refresh_tokens ADD COLUMN updated_by UUID;
ALTER TABLE refresh_tokens ADD COLUMN deleted_at TIMESTAMP NULL;
