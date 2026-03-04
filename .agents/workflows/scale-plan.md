---
description: /scale-plan <traffic-profile>
---

# WORKFLOW RULES MODE ACTIVE

Workspace: Intern Management Backend.
Base package: `com.example.backend`.
Stack: Spring Boot + PostgreSQL + Docker.

## Enforcement

- Output only the sections defined below.
- No narrative. No recommendations unless explicitly requested.
- If required input is missing, state: "Không đủ dữ liệu xác minh".
- No speculative capacity claims.
- Tradeoffs must be concrete with quantifiable impact.
- Terminate output immediately after completion.

---

## Step 1 — Analyze Traffic Pattern

- Requests per second (RPS).
- Peak time identification.
- Read vs write ratio per endpoint category (intern, task, review).
- User distribution by role (ADMIN, HR_MANAGER, MENTOR, INTERN).

## Step 2 — Application Scaling Strategy

- Horizontal scaling via container replicas.
- CPU/memory thresholds for auto-scaling.
- Minimum and maximum replica count.
- JVM tuning (heap, GC strategy).
- Tomcat thread pool configuration.

## Step 3 — Database Scaling

- PostgreSQL read replica strategy.
- Table partitioning for high-volume tables.
- HikariCP connection pool sizing.
- Index optimization for scaled load.

## Step 4 — Caching Strategy

- Entities to cache (frequency-based selection).
- TTL per entity type.
- Cache invalidation strategy.

## Step 5 — Cost vs Performance Tradeoff

- Quantified cost per scaling tier.
- Performance gain per resource increment.
- Diminishing returns threshold.

## Required Output Sections

| Section | Required |
|---|---|
| Architecture diagram (text) | Yes |
| Bottleneck risks (evidence-based) | Yes |
| Capacity estimation logic | Yes |
| Failure scenarios | Yes |
| Rollback strategy | Yes |
| Isolation boundaries | Yes |
| Observability impact | Yes |

If any section is not applicable, mark: "Not applicable in current scope".