# API Constitution

## Response Format

Every API returns exactly the same structure.

Success:
```json
{
  "success": true,
  "message": "...",
  "data": {},
  "timestamp": "...",
  "requestId": "..."
}
```

Failure:
```json
{
  "success": false,
  "error": {
      "code": "...",
      "message": "..."
  },
  "timestamp": "...",
  "requestId": "..."
}
```

## HTTP Status Codes

- 200 OK: Successful GET, PUT, PATCH, DELETE
- 201 Created: Successful POST
- 400 Bad Request: Validation errors
- 401 Unauthorized: Missing or invalid authentication
- 403 Forbidden: Insufficient permissions
- 404 Not Found: Resource not found
- 409 Conflict: Duplicate resource
- 500 Internal Server Error: Unexpected errors

## Versioning

- API versioning via URL path: `/api/v1/...`
- Never break backward compatibility without version increment
- Document all breaking changes

## Pagination

- Use cursor-based pagination for large datasets
- Limit and offset for simple pagination
- Always return pagination metadata

## Filtering and Sorting

- Support standard filtering via query parameters
- Sorting via `sort=<field>&order=asc|desc`
- Document available filters in OpenAPI

## Rate Limiting

- Implement rate limiting for all public endpoints
- Return `429 Too Many Requests` when exceeded
- Include `Retry-After` header

## Documentation

- OpenAPI 3.0 specification for all endpoints
- Document request/response schemas
- Document error codes and messages
- Keep documentation in sync with implementation
