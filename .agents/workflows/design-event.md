---
description: /design-event <event-name>
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

## Step 1 — Define Purpose

- Why this event exists within intern management domain.
- Which component publishes it.
- Which components consume it.

## Step 2 — Define Schema

Required fields:

| Field | Type | Description |
|---|---|---|
| `version` | String | Schema version |
| `timestamp` | String | ISO 8601 format |
| `correlationId` | String | Request trace identifier |
| `data` | Object | Event-specific payload |

## Step 3 — Idempotency Strategy

- Unique event ID generation mechanism.
- Duplicate prevention logic at consumer side.

## Step 4 — Failure Handling

- Retry policy: max attempts, backoff strategy.
- Dead-letter queue strategy.
- Failure logging requirements.

## Step 5 — Versioning Strategy

- Backward compatibility enforcement.
- No schema change without version bump.

## Step 6 — Performance Impact

- Expected event volume.
- Processing throughput impact.

## Step 7 — Spring Integration

- Use `ApplicationEventPublisher` for intra-service events.
- `@Async` for non-critical listeners.
- Cross-cutting logging via `aspect/` package.

## Required Output Sections

| Section | Required |
|---|---|
| JSON schema | Yes |
| Risk analysis (evidence-based) | Yes |
| Failure scenarios | Yes |
| Rollback strategy | Yes |
| Isolation boundaries | Yes |
| Scaling considerations | Yes |
| Observability impact | Yes |

If any section is not applicable, mark: "Not applicable in current scope".