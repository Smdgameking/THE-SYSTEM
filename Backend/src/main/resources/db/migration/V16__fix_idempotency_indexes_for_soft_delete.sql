-- ============================================================================
-- V16: Fix idempotency indexes to respect soft deletes
-- ============================================================================
-- V15 created partial unique indexes without filtering on deleted_at IS NULL.
-- This migration corrects those indexes so that soft-deleted transactions
-- and rewards do not permanently reserve their source identity.

-- Drop the old indexes if they exist.
DROP INDEX IF EXISTS uq_xp_transactions_source;
DROP INDEX IF EXISTS uq_reward_history_source;

-- Recreate with soft-delete exclusion.
CREATE UNIQUE INDEX IF NOT EXISTS uq_xp_transactions_source
ON xp_transactions(source_engine, source_id, source_type)
WHERE source_engine IS NOT NULL
  AND source_id IS NOT NULL
  AND source_type IS NOT NULL
  AND deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_reward_history_source
ON reward_history(user_id, source_type, source_id)
WHERE source_type IS NOT NULL
  AND source_id IS NOT NULL
  AND deleted_at IS NULL;
