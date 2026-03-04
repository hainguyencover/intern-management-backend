---
description: /create-service <service-name>
---

# WORKFLOW RULES MODE ACTIVE

Workspace: Intern Management Backend.
Base package: `com.example.backend`.
Stack: Spring Boot + PostgreSQL + Docker.

## Enforcement

- Output only the sections defined below.
- No narrative. No recommendations unless explicitly requested.
- If required input is missing, state: "Không đủ dữ liệu xác minh".
- All reasoning must be system-level and deterministic.
- Terminate output immediately after completion.

---

## Step 1 — Define Service Scope

- Business responsibility within intern management domain.
- Bounded context boundaries.
- Non-functional requirements (latency, throughput, availability).

## Step 2 — Define Package Structure

- Controller: `controller/` — REST endpoints only.
- Service: `service/` (interface), `service/impl/` (implementation).
- Repository: `repository/` — Spring Data JPA.
- Entity: `entity/` — JPA entities, never exposed.
- DTO: `dto/request/`, `dto/response/`, `dto/admin/`, `dto/hrm/`.
- Enums: `enums/`.
- Exception: `exception/` — custom exceptions + `GlobalExceptionHandler`.
- Config: `config/`.
- Security: `security/`.
- Aspect: `aspect/` — AOP cross-cutting concerns.

## Step 3 — Define API Contract

- OpenAPI specification required.
- Versioned endpoints: `/api/v1/`.
- Pagination for all list endpoints.
- Standardized response: `ApiResponse<T>`.
- Error response: `status`, `message`, `timestamp`, `path`.

## Step 4 — Define Database

- PostgreSQL.
- Flyway migrations: `V{version}__{description}.sql`.
- Tables with: `id`, `created_at`, `updated_at`, `created_by`, `updated_by`.
- Soft delete: `is_deleted` or `status`.
- Foreign keys indexed.
- `snake_case` naming.

## Step 5 — Define Events

- Events produced by this service.
- Events consumed by this service.
- Naming: `<domain>.<entity>.<action>`.
- Payload: `version`, `timestamp`, `correlationId`, `data`.
- Published only after transaction commit.

## Step 6 — Security Design

- JWT claims: `user_id`, `role`, `email`.
- RBAC roles: ADMIN, HR_MANAGER, MENTOR, INTERN.
- Authorization at service layer via `@PreAuthorize`.
- BCrypt password hashing.

## Step 7 — Observability

- Structured JSON logging.
- No sensitive data in logs.
- Spring Actuator: `/actuator/health`, `/actuator/metrics`.

## Step 8 — DevOps

- Multi-stage Dockerfile.
- Image tag: `version + commit hash`.
- CI: lint → build → test → coverage ≥ 70%.

## Required Output Sections

| Section | Required |
|---|---|
| Architecture summary | Yes |
| Risks | Yes |
| Failure scenarios | Yes |
| Rollback plan | Yes |
| Scaling considerations | Yes |
| Isolation boundaries | Yes |
| Observability impact | Yes |

If any section is not applicable, mark: "Not applicable in current scope".