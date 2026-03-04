---
description: /production-audit
---

# WORKFLOW RULES MODE ACTIVE

Workspace: Intern Management Backend.
Base package: `com.example.backend`.
Stack: Spring Boot + PostgreSQL + Docker.

## Enforcement

- Output only the sections defined below.
- No narrative. No recommendations unless explicitly requested.
- If required input is missing, state: "Không đủ dữ liệu xác minh".
- All findings must be evidence-based.
- No speculative architecture claims.
- Terminate output immediately after completion.

---

## Audit Category 1 — Architecture

- Package structure follows domain-based convention.
- Controller → Service → Repository layering enforced.
- No layer skipping (controllers accessing repositories).
- Services return DTOs only — entities never exposed.
- No circular dependencies between packages.

## Audit Category 2 — Security

- HTTPS enforced in production.
- JWT validation implemented in `security/` package.
- RBAC roles enforced: ADMIN, HR_MANAGER, MENTOR, INTERN.
- Authorization at service layer via `@PreAuthorize`.
- BCrypt password hashing (minimum 10 rounds).
- Secrets via environment variables — no hardcoded values.
- CORS whitelist — no wildcard `*` in production.

## Audit Category 3 — Database

- PostgreSQL with Flyway migrations.
- Index coverage on foreign keys.
- Slow query detection (> 100ms threshold).
- Audit columns: `created_at`, `updated_at`, `created_by`, `updated_by`.
- Soft delete implemented.
- No `SELECT *` in production code.
- N+1 query patterns eliminated.

## Audit Category 4 — Observability

- Structured JSON logging.
- `correlationId` in all request flows.
- Spring Actuator: `/actuator/health`, `/actuator/metrics`.
- No sensitive data in logs.

## Audit Category 5 — DevOps

- CI pipeline: lint → build → test.
- Test coverage ≥ 70%.
- Blue/Green or Rolling deployment.
- Docker image tagged with version + commit hash.
- Multi-stage Dockerfile.

## Audit Category 6 — API Standards

- All endpoints versioned: `/api/v1/`.
- `ApiResponse<T>` wrapper on all responses.
- Pagination on all list endpoints.
- Error responses include: `status`, `message`, `timestamp`, `path`.
- HTTP status codes semantically correct.

## Required Output Format

| Section | Required |
|---|---|
| Critical issues | Yes |
| Medium issues | Yes |
| Low issues | Yes |
| Failure scenarios | Yes |
| Rollback strategy | Yes |
| Isolation boundaries | Yes |
| Immediate action plan | Yes |
| Observability impact | Yes |

If any section is not applicable, mark: "Not applicable in current scope".