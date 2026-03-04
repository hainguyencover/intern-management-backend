---
description: /security-penetration-plan
---

# WORKFLOW RULES MODE ACTIVE

Workspace: Intern Management Backend.
Base package: `com.example.backend`.
Stack: Spring Boot + PostgreSQL + Docker.

## Enforcement

- Output only the sections defined below.
- No narrative. No recommendations unless explicitly requested.
- If required input is missing, state: "Không đủ dữ liệu xác minh".
- All attack vectors must be concrete and evidence-based.
- No fabricated vulnerability data.
- Terminate output immediately after completion.

---

## Phase 1 — Authentication Testing

- JWT manipulation (signature tampering, algorithm switching).
- Expired token bypass attempt.
- Token replay attack.
- Refresh token abuse.

## Phase 2 — Authorization Testing

- Role escalation: INTERN → MENTOR → HR_MANAGER → ADMIN.
- Horizontal privilege escalation (accessing other user data).
- Vertical privilege escalation (accessing admin endpoints).
- `@PreAuthorize` bypass at service layer.

## Phase 3 — API Security

- SQL injection via JPQL/native queries.
- XSS via DTO input fields.
- Mass assignment via unvalidated request DTOs.
- Rate limit bypass on public endpoints.
- CORS misconfiguration exploitation.

## Phase 4 — Data Security

- Sensitive data exposure in API responses (passwords, tokens).
- Sensitive data in application logs.
- Secrets in source code or Docker images.
- BCrypt hash strength validation.

## Phase 5 — Infrastructure Security

- Docker container escape test.
- Secret exposure in environment variables.
- PostgreSQL connection security.
- Actuator endpoint exposure.

## Phase 6 — Event Security

- Event replay attack via `ApplicationEventPublisher`.
- Duplicate message processing.
- Event schema manipulation.

## Required Output Sections

| Section | Required |
|---|---|
| Vulnerability categories | Yes |
| Attack vector analysis | Yes |
| Risk severity classification (Critical/High/Medium/Low) | Yes |
| Failure scenarios | Yes |
| Mitigation roadmap | Yes |
| Rollback strategy | Yes |
| Isolation boundaries | Yes |
| Observability impact | Yes |

If any section is not applicable, mark: "Not applicable in current scope".