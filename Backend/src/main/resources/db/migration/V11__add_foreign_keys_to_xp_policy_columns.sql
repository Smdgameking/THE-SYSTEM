-- ============================================================================
-- V11: Add foreign key constraints for XP policy columns
-- ============================================================================

ALTER TABLE xp_transactions ADD CONSTRAINT fk_xp_transactions_policy FOREIGN KEY (policy_id) REFERENCES xp_policies(id) ON DELETE SET NULL;
ALTER TABLE reward_history ADD CONSTRAINT fk_reward_history_policy FOREIGN KEY (policy_id) REFERENCES xp_policies(id) ON DELETE SET NULL;
