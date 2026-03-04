---
trigger: always_on
---

# Frontend Integration Rules — Intern Management Backend

## API Contract

1. Backend must provide OpenAPI/Swagger documentation for all endpoints.
2. All API responses must use standardized `ApiResponse<T>` wrapper.
3. Error responses must include: `status`, `message`, `timestamp`, `path`.
4. No breaking API changes without version bump (`/api/v1/` → `/api/v2/`).

## CORS Configuration

5. CORS must be configured in `config/` package.
6. Allowed origins: explicitly listed — no wildcard `*` in production.
7. Allowed methods: GET, POST, PUT, PATCH, DELETE, OPTIONS.
8. Credentials support must be enabled for JWT cookie-based flows.

## Authentication Contract

9. Login endpoint returns JWT access token + refresh token.
10. Token format and claims documented in API spec.
11. Role information included in token for frontend route guard decisions.

## Data Contract

12. Date/time fields: ISO 8601 format (`yyyy-MM-dd'T'HH:mm:ss`).
13. Pagination response includes: `content`, `totalElements`, `totalPages`, `currentPage`, `pageSize`.
14. Enum values returned as string names, not ordinal numbers.

## Error Handling

15. HTTP status codes must be semantically correct:
    - 200: success.
    - 201: created.
    - 400: validation error.
    - 401: unauthorized.
    - 403: forbidden.
    - 404: not found.
    - 500: internal server error.