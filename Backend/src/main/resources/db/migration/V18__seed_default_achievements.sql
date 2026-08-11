-- ============================================================================
-- V18: Seed default streak achievements
-- ============================================================================
-- Inserts the baseline STREAK achievements used by the XP engine's achievement
-- evaluation. This migration is idempotent: if an achievement with the same code
-- already exists, it is skipped.

INSERT INTO achievement_definitions (
    id,
    code,
    name,
    description,
    category,
    requirement_type,
    requirement_value,
    xp_reward,
    is_hidden,
    is_repeatable,
    sort_order,
    created_at,
    updated_at
)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'STREAK_3_DAY',
    '3-Day Streak',
    'Maintain a 3-day streak',
    'STREAK',
    'STREAK',
    '{"metric":"current_streak","milestone":3}',
    50,
    false,
    false,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO achievement_definitions (
    id,
    code,
    name,
    description,
    category,
    requirement_type,
    requirement_value,
    xp_reward,
    is_hidden,
    is_repeatable,
    sort_order,
    created_at,
    updated_at
)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    'STREAK_7_DAY',
    '7-Day Streak',
    'Maintain a 7-day streak',
    'STREAK',
    'STREAK',
    '{"metric":"current_streak","milestone":7}',
    200,
    false,
    false,
    2,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO achievement_definitions (
    id,
    code,
    name,
    description,
    category,
    requirement_type,
    requirement_value,
    xp_reward,
    is_hidden,
    is_repeatable,
    sort_order,
    created_at,
    updated_at
)
VALUES (
    '33333333-3333-3333-3333-333333333333',
    'STREAK_14_DAY',
    '14-Day Streak',
    'Maintain a 14-day streak',
    'STREAK',
    'STREAK',
    '{"metric":"current_streak","milestone":14}',
    500,
    false,
    false,
    3,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (code) DO NOTHING;

INSERT INTO achievement_definitions (
    id,
    code,
    name,
    description,
    category,
    requirement_type,
    requirement_value,
    xp_reward,
    is_hidden,
    is_repeatable,
    sort_order,
    created_at,
    updated_at
)
VALUES (
    '44444444-4444-4444-4444-444444444444',
    'STREAK_30_DAY',
    '30-Day Streak',
    'Maintain a 30-day streak',
    'STREAK',
    'STREAK',
    '{"metric":"current_streak","milestone":30}',
    1000,
    false,
    false,
    4,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (code) DO NOTHING;
