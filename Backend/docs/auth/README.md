# Auth Module

## Purpose

The Auth module handles user authentication, authorization, and token management. It provides the security foundation for THE SYSTEM.

## Database Tables

### users
- `id` UUID PRIMARY KEY
- `email` VARCHAR(255) UNIQUE NOT NULL
- `password_hash` VARCHAR(255) NOT NULL
- `email_verified` BOOLEAN NOT NULL DEFAULT FALSE
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL
- `created_by` UUID
- `updated_by` UUID
- `deleted_at` TIMESTAMP NULL

### roles
- `id` UUID PRIMARY KEY
- `name` VARCHAR(50) UNIQUE NOT NULL
- `description` TEXT
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL
- `created_by` UUID
- `updated_by` UUID
- `deleted_at` TIMESTAMP NULL

### user_roles
- `user_id` UUID (composite PK)
- `role_id` UUID (composite PK)
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL
- `created_by` UUID
- `updated_by` UUID

## API Endpoints

| Method | Endpoint            | Description        | Auth Required |
|--------|---------------------|--------------------|---------------|
| POST   | /api/auth/register  | Register new user  | No            |
| POST   | /api/auth/login     | Login user         | No            |
| POST   | /api/auth/refresh   | Refresh access token | No          |
| POST   | /api/auth/logout    | Logout user        | No            |

## Business Rules

1. Email must be unique across all users
2. Password must be at least 8 characters
3. Email verification is required for full account access
4. New users get ROLE_USER by default
5. JWT access tokens expire in 15 minutes
6. JWT refresh tokens expire in 7 days
7. Refresh tokens are rotated on each use

## Relationships

- User belongs to many Roles (many-to-many)
- Role belongs to many Users (many-to-many)
- Settings belong to User (one-to-many)

## Events

- `UserRegisteredEvent` - fired when user registers
- `UserLoggedInEvent` - fired on successful login
- `UserLoggedOutEvent` - fired on logout

## Future Roadmap

- Email verification flow
- Password reset functionality
- OAuth2 integration (Google, GitHub)
- Two-factor authentication (2FA)
- Account deletion with data anonymization
- Session management
- Login history
