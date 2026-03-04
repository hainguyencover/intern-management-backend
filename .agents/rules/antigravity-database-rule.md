---
trigger: always_on
---

# Database Rules — Intern Management Backend

## Schema Management

1. Database: MySQL.
2. All schema changes must use Flyway migrations (`db/migration/`).
3. Migration files follow naming: `V{version}__{description}.sql`.
4. Migrations must never be modified after applied to any environment.
5. Rollback migrations (`U{version}__`) required for production-critical changes.

## Table Design

6. All tables must have a primary key (`id`).
7. All foreign keys must have corresponding indexes.
8. Audit columns required: `created_at`, `updated_at`, `created_by`, `updated_by`.
9. Soft delete column required for business tables (`is_deleted` or `status`).
10. Use `snake_case` for all column and table names.

## Query Performance

11. No `SELECT *` in production code.
12. Queries over 100ms must be optimized with explain plan.
13. Pagination required for all list queries.
14. N+1 query patterns must be avoided — use `JOIN FETCH` or `@EntityGraph`.

## Data Integrity

15. Constraints must be enforced at database level (NOT NULL, UNIQUE, CHECK).
16. Application-level validation does not replace database constraints.
17. Enum values stored as `VARCHAR`, not ordinal integers.