-- ============================================================================
-- V10: Add missing audit fields to task tables
-- ============================================================================

-- task_dependencies missing updated_at and updated_by
ALTER TABLE task_dependencies ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE task_dependencies ADD COLUMN updated_by UUID;

-- task_time_entries missing updated_at and updated_by
ALTER TABLE task_time_entries ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE task_time_entries ADD COLUMN updated_by UUID;

-- recurring_task_configs missing updated_by and deleted_at
ALTER TABLE recurring_task_configs ADD COLUMN updated_by UUID;
ALTER TABLE recurring_task_configs ADD COLUMN deleted_at TIMESTAMP NULL;
