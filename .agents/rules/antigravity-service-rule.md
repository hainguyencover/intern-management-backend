---
trigger: always_on
---

# Service Development Rules — Intern Management Backend

## Controller Layer

1. Controllers must not contain business logic.
2. Controllers only handle HTTP mapping, input binding, and delegation to service.
3. All controller methods must return `ResponseEntity<ApiResponse<T>>`.
4. Request validation via `@Valid` and Bean Validation annotations.

## Service Layer

5. Services must validate all business rules.
6. Service interfaces in `service/`, implementations in `service/impl/`.
7. All service methods must use DTOs — entities must never be exposed.
8. Service methods must be `@Transactional` where applicable.

## Repository Layer

9. Repository interfaces extend `JpaRepository` or `JpaSpecificationExecutor`.
10. No native queries unless performance-critical and documented.
11. Custom query methods must use `@Query` with JPQL.

## Entity Layer

12. Entities must use Lombok `@Getter`, `@Setter`, `@Builder` where applicable.
13. Soft delete required for business entities (`deleted` or `status` field).
14. Audit fields required: `createdAt`, `updatedAt`, `createdBy`, `updatedBy`.
15. No `SELECT *` in production code — use projections or DTOs.

## DTO Layer

16. DTOs organized by context: `request/`, `response/`, `admin/`, `hrm/`.
17. Mapper layer required for Entity ↔ DTO conversion (MapStruct or manual).
18. No business logic inside DTOs.

## General

19. Pagination required for list endpoints returning > 100 records.
20. All services must expose health check via `/actuator/health`.
21. No hardcoded secrets — use `application.yml` or environment variables.