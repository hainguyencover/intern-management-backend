---
description: /performance-benchmark <service-name>
---

# WORKFLOW RULES MODE ACTIVE

Workspace: Intern Management Backend.
Base package: `com.example.backend`.
Stack: Spring Boot + PostgreSQL + Docker.

## Enforcement

- Output only the sections defined below.
- No narrative. No recommendations unless explicitly requested.
- If required input is missing, state: "Không đủ dữ liệu xác minh".
- No invented metrics or benchmarks.
- All performance claims must be evidence-based.
- Terminate output immediately after completion.

---

## Step 1 — Define Performance Metrics

- Requests per second (RPS).
- P50, P95, P99 latency.
- Error rate (4xx, 5xx).
- Throughput (MB/s).

## Step 2 — Define Test Types

| Test Type | Purpose |
|---|---|
| Load test | Sustained normal traffic |
| Stress test | Beyond capacity limit |
| Spike test | Sudden traffic burst |
| Soak test | Extended duration stability |

## Step 3 — Define Traffic Model

- Concurrent users.
- Read/write ratio per endpoint.
- Endpoint distribution (CRUD operations on intern, task, review domains).

## Step 4 — Identify Bottlenecks

- CPU utilization (JVM).
- Memory (heap, GC pressure).
- HikariCP connection pool saturation.
- Tomcat thread pool exhaustion.
- N+1 query patterns in JPA.

## Step 5 — Optimization Strategy

- PostgreSQL index tuning.
- JPQL query optimization.
- `@EntityGraph` for N+1 elimination.
- Application-level caching.
- Horizontal scaling via container replicas.

## Step 6 — Reporting

- Baseline performance numbers.
- Scaling curve (RPS vs latency vs replicas).
- Degradation threshold identification.

## Required Output Sections

| Section | Required |
|---|---|
| Test plan | Yes |
| Bottleneck risk (evidence-based) | Yes |
| Capacity planning formula | Yes |
| Failure scenarios | Yes |
| Rollback strategy | Yes |
| Isolation boundaries | Yes |
| Production readiness recommendation | Yes |
| Observability impact | Yes |

If any section is not applicable, mark: "Not applicable in current scope".