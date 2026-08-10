-- ============================================================================
-- V13: Seed default XP policies
-- ============================================================================
-- Inserts the documented baseline XP policies. This migration is idempotent:
-- if a policy with the same code already exists, it is skipped.

INSERT INTO xp_policies (id, code, name, description, policy_type, base_xp, multiplier, conditions, is_active, priority, created_at, updated_at)
VALUES (
    'a1b2c3d4-1111-2222-3333-444455556666',
    'TASK_COMPLETION',
    'Task Completion',
    'Base XP awarded for completing a task',
    'TASK_COMPLETION',
    10,
    1.0,
    '{"priority_multipliers":{"LOW":1.0,"NORMAL":1.0,"HIGH":1.5,"CRITICAL":2.0},"difficulty_multipliers":{"EASY":1.0,"NORMAL":1.25,"HARD":1.5,"EXTREME":2.0},"streak_bonus":{"enabled":true,"milestones":[3,7,14,30,60,90],"multipliers":[1.1,1.25,1.5,2.0,2.5,3.0]}}',
    true,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO xp_policies (id, code, name, description, policy_type, base_xp, multiplier, conditions, is_active, priority, created_at, updated_at)
VALUES (
    'a1b2c3d4-2222-3333-4444-555566667777',
    'GOAL_COMPLETION',
    'Goal Completion',
    'Base XP awarded for completing a goal',
    'GOAL_COMPLETION',
    100,
    1.0,
    '{}',
    true,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (code) DO NOTHING;
