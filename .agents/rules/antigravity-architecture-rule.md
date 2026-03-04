---
trigger: always_on
---

# Architecture Rules — Intern Management Backend

## Package Structure

1. Base package: `com.example.backend`.
2. Domain-based packaging required:
   - `controller/` — REST endpoints only, no business logic.
   - `service/` — Business logic interfaces.
   - `service/impl/` — Business logic implementations.
   - `repository/` — Data access layer (Spring Data JPA).
   - `entity/` — JPA entities, never exposed to API consumers.
   - `dto/` — Request/Response DTOs grouped by context (`request/`, `response/`, `admin/`, `hrm/`).
   - `enums/` — Enum constants.
   - `exception/` — Custom exceptions and global handler.
   - `config/` — Spring configuration classes.
   - `security/` — Authentication and authorization.
   - `aspect/` — Cross-cutting concerns (AOP).

## Layering Rules

3. Controller → Service → Repository. No layer skipping.
4. Controllers must not access repositories directly.
5. Services must not return entities — only DTOs.
6. No circular dependencies between packages.

## API Design

7. All REST APIs must follow RESTful conventions.
8. API versioning: `/api/v1/` prefix required.
9. Standardized response format via `ApiResponse<T>` wrapper.
10. All list endpoints must support pagination.