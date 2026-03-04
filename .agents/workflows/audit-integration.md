---
description: Workflow for implementing systemic audit and configuration hooks.
---

# Implement Audit & Configuration Hooks

Use this workflow to implement cross-cutting auditing and dynamic configuration.

## Step 1: Bootstrap Infrastructure
1. Create the `AuditLog` entity in `com.example.backend.entity`.
2. Create `AuditLogRepository`.
3. Create the `SystemConfig` entity and repository.

## Step 2: Implement Audit Aspect
1. Create `AuditAspect` in `com.example.backend.aspect`.
2. Define a pointcut for methods annotated with a custom `@Auditable` annotation or for service layer write operations.
3. Implement `@Around` advice to capture entity state changes.
// turbo
4. Add the custom `@Auditable` annotation to the `annotation` package.

## Step 3: Implement Configuration Service
1. Create `SystemConfigService` interface.
2. Implement `SystemConfigServiceImpl` with a cache-aside pattern (e.g., using Spring Cache or a simple Local Cache).
3. Expose management endpoints in a `SystemConfigController` for ADMIN role.

## Step 4: Verification
1. Perform CRUD operations on an entity (e.g., `InternProfile`).
2. Verify that `audit_logs` are generated correctly.
3. Test `SystemConfig` retrieval and update.
