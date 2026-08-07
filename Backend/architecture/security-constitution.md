# Security Constitution

## Authentication

- JWT-based authentication
- Access tokens: short-lived (15 minutes)
- Refresh tokens: long-lived (7 days)
- Tokens stored in HTTP-only, Secure cookies
- Never store tokens in localStorage

## Authorization

- Role-Based Access Control (RBAC)
- Every endpoint requires explicit authorization
- Default deny: all endpoints protected unless explicitly public

## Password Security

- BCrypt with cost factor 12
- Never store plain text passwords
- Never log passwords or password hashes
- Enforce password complexity rules

## Token Security

- Never log access or refresh tokens
- Tokens signed with HS256 using strong secret (256+ bits)
- Refresh tokens rotated on each use
- Tokens invalidated on logout

## Headers

- `Authorization: Bearer <token>` for API calls
- CORS configured for specific origins in production
- Security headers: CSP, X-Frame-Options, X-Content-Type-Options

## Secrets Management

- Never commit secrets to version control
- Use environment variables or secret management service
- Rotate secrets regularly
- Different secrets for each environment

## Audit Logging

- Log all authentication events (login, logout, failed attempts)
- Log all authorization failures
- Never log sensitive data in audit logs
