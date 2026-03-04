---
trigger: always_on
---

# Security Rules — Intern Management Backend

## Authentication

1. JWT-based authentication required.
2. JWT must include: `user_id`, `role`, `email`.
3. Token expiration must be configured — no infinite tokens.
4. Refresh token mechanism required for session continuity.

## Authorization

5. Role-based access control (RBAC) enforced.
6. Roles: ADMIN, HR_MANAGER, MENTOR, INTERN (or as defined in `enums/`).
7. Role check must happen at service layer via `@PreAuthorize` or custom annotation.
8. Controller-level `@Secured` only as supplementary — service layer is authoritative.

## Data Protection

9. HTTPS only in production.
10. Passwords must be hashed with BCrypt (minimum 10 rounds).
11. Secrets must come from environment variables or Spring profiles — never hardcoded.
12. Sensitive data (passwords, tokens) must never appear in logs or API responses.

## Audit

13. All critical actions must generate audit logs (login, data modification, role changes).
14. Audit logs must include: `userId`, `action`, `timestamp`, `ipAddress`.

## API Security

15. CORS configuration must whitelist specific domains — no wildcard `*` in production.
16. Rate limiting recommended for public endpoints.
17. Input sanitization required to prevent SQL injection and XSS.