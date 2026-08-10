-- ============================================================================
-- V14: Add difficulty column to tasks table
-- ============================================================================

ALTER TABLE tasks ADD COLUMN IF NOT EXISTS difficulty VARCHAR(20) NULL;

UPDATE tasks SET difficulty = 'NORMAL' WHERE difficulty IS NULL;
