---
description: /cost-optimization <environment>
---

# WORKFLOW RULES MODE ACTIVE

Workspace: Intern Management Backend.
Base package: `com.example.backend`.
Stack: Spring Boot + PostgreSQL + Docker.

## Enforcement

- Output only the sections defined below.
- No narrative. No recommendations unless explicitly requested.
- If required input is missing, state: "Không đủ dữ liệu xác minh".
- No speculative cost estimates — use concrete data only.
- Tradeoffs must be concrete, not theoretical.
- Terminate output immediately after completion.

---

## Step 1 — Identify Cost Drivers

- Compute (application containers).
- Database (PostgreSQL instances).
- Storage (volumes, backups).
- Network (inter-service, external).

## Step 2 — Compute Optimization

- Auto-scaling threshold tuning.
- Right-size container resources (CPU/memory limits).
- JVM heap and GC tuning for Spring Boot.

## Step 3 — Database Optimization

- Remove unused indexes.
- Query optimization via `EXPLAIN ANALYZE`.
- Read replica reduction if underutilized.
- Connection pool tuning (HikariCP).

## Step 4 — Caching Optimization

- Reduce DB hit ratio via application-level caching.
- TTL tuning per entity type.
- Cache eviction policy.

## Step 5 — Storage Optimization

- Docker image size reduction (multi-stage build).
- Log rotation policy.
- Backup retention policy.

## Step 6 — Cost vs Reliability Tradeoff

- Quantified tradeoff per optimization item.
- No theoretical statements — data-backed only.

## Required Output Sections

| Section | Required |
|---|---|
| Cost reduction strategy | Yes |
| Risk impact (evidence-based) | Yes |
| Estimated savings logic | Yes |
| Failure scenarios | Yes |
| Rollback safety plan | Yes |
| Isolation boundaries | Yes |
| Observability impact | Yes |

If any section is not applicable, mark: "Not applicable in current scope".