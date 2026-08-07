# User Module

## Purpose

The User module manages user profiles and account information. It is responsible for profile data, display settings, and user preferences. It does NOT handle authentication.

## Database Tables

### user_profiles
- `id` UUID PRIMARY KEY
- `user_id` UUID UNIQUE NOT NULL (FK to users.id)
- `username` VARCHAR(50) UNIQUE
- `display_name` VARCHAR(100)
- `bio` TEXT
- `avatar_url` VARCHAR(500)
- `timezone` VARCHAR(50)
- `locale` VARCHAR(10)
- `country` VARCHAR(2)
- `account_status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
- `last_active_at` TIMESTAMP
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL
- `created_by` UUID
- `updated_by` UUID
- `deleted_at` TIMESTAMP NULL

## API Endpoints

| Method | Endpoint            | Description         | Auth Required |
|--------|---------------------|---------------------|---------------|
| GET    | /users/me           | Get my profile      | Yes           |
| PUT    | /users/me           | Update my profile   | Yes           |
| GET    | /users/{id}         | Get public profile  | Yes           |
| GET    | /users/username/{username} | Get public profile by username | Yes |

## Business Rules

1. Users can only update their own profile
2. Username must be unique across all users
3. Username must be 3-50 characters
4. Display name must not exceed 100 characters
5. Bio must not exceed 500 characters
6. Avatar URL must not exceed 500 characters
7. Timezone must not exceed 50 characters
8. Locale must not exceed 10 characters
9. Country code must not exceed 2 characters
10. Profile is created lazily when first accessed
11. Last active timestamp is updated on profile retrieval

## Relationships

- user_profiles belongs to users (one-to-one)
- user_profiles.user_id references users.id

## Events

- `UserProfileUpdatedEvent` - fired when profile is updated
- `UserProfileCreatedEvent` - fired when profile is created

## Future Roadmap

- Avatar upload and storage
- Profile picture cropping
- Custom profile fields
- Profile visibility settings
- User preferences (notifications, theme, etc.)
- Account deletion and data export
- Profile verification badges
