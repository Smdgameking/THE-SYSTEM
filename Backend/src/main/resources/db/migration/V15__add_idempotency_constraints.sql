-- ============================================================================
-- V15: Add idempotency constraints to XP tables
-- ============================================================================
-- Enforces uniqueness at the database level to prevent duplicate XP awards
-- from retried or replayed events.

-- First, verify no existing duplicates would violate the constraint.
-- If duplicates exist, the migration will fail and must be resolved manually.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM xp_transactions
        WHERE deleted_at IS NULL
          AND source_engine IS NOT NULL
          AND source_id IS NOT NULL
          AND source_type IS NOT NULL
        GROUP BY source_engine, source_id, source_type
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate xp_transactions found for source identity. Cannot create unique constraint.';
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_xp_transactions_source
ON xp_transactions(source_engine, source_id, source_type)
WHERE source_engine IS NOT NULL
  AND source_id IS NOT NULL
  AND source_type IS NOT NULL;

-- Prevent duplicate reward grants for the same user/source combination.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM reward_history
        WHERE deleted_at IS NULL
          AND source_type IS NOT NULL
          AND source_id IS NOT NULL
        GROUP BY user_id, source_type, source_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate reward_history entries found. Cannot create unique constraint.';
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_reward_history_source
ON reward_history(user_id, source_type, source_id)
WHERE source_type IS NOT NULL
  AND source_id IS NOT NULL;
