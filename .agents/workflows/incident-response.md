---
description: /incident-response <incident-description>
---

# WORKFLOW RULES MODE ACTIVE

Workspace: Intern Management Backend.
Base package: `com.example.backend`.
Stack: Spring Boot + PostgreSQL + Docker.

## Enforcement

- Output only the sections defined below.
- No narrative. No recommendations unless explicitly requested.
- If required input is missing, state: "Không đủ dữ liệu xác minh".
- Timeline must be factual — no speculative entries.
- Root cause must be evidence-based.
- Terminate output immediately after completion.

---

## Step 1 — Incident Classification

| Field | Required |
|---|---|
| Severity | P1 / P2 / P3 |
| Affected components | Controller / Service / Repository / Database / Security |
| Impact scope | Users affected, endpoints degraded |

## Step 2 — Immediate Containment

- Freeze deployments.
- Activate fallback (if configured).
- Rate limiting on affected endpoints.
- Disable affected scheduled tasks / event listeners.

## Step 3 — Diagnosis

- Log inspection via `correlationId`.
- Spring Actuator metrics anomaly check (`/actuator/metrics`).
- PostgreSQL query performance check (`pg_stat_activity`).
- HikariCP connection pool status.
- Event flow verification.

## Step 4 — Mitigation

- Rollback to previous Docker image version.
- Restart application containers.
- Scale containers if overload detected.
- Apply Flyway rollback migration if DB change caused issue.

## Step 5 — Root Cause Analysis

- Technical cause (code, query, config, dependency).
- Process failure (missing test, review gap).
- Monitoring gap (missing alert, missing metric).

## Step 6 — Prevention Plan

- Code fix with corresponding test.
- Monitoring rule addition.
- CI/CD governance improvement.

## Required Output Sections

| Section | Required |
|---|---|
| Timeline (factual) | Yes |
| Technical summary | Yes |
| Failure scenarios | Yes |
| Rollback strategy | Yes |
| Isolation boundaries | Yes |
| Preventive action list | Yes |
| Observability impact | Yes |

If any section is not applicable, mark: "Not applicable in current scope".