---
description: /design-saga <business-process-name>
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
- Tradeoffs must be concrete, not theoretical.
- Terminate output immediately after completion.

---

## Step 1 — Define Business Process

- Full workflow steps within intern management domain.
- Services/components involved.
- Success outcome.
- Failure outcome.

## Step 2 — Choose Saga Type

- Orchestration or Choreography.
- Justification must be evidence-based.

## Step 3 — Define Transaction Steps

For each step:

| Field | Required |
|---|---|
| Service/Component | Yes |
| Action | Yes |
| Success event | Yes |
| Failure event | Yes |
| Compensation action | Yes |

## Step 4 — Compensation Logic

- Reverse DB changes via Flyway-compatible rollback.
- Idempotency guarantee per step.
- Retry strategy with max attempts and backoff.

## Step 5 — Failure Handling

- Timeout strategy per step.
- Dead-letter handling mechanism.
- Partial failure resolution logic.

## Step 6 — Observability

- `correlationId` flow through all steps.
- Saga state tracking (in-progress, completed, compensating, failed).
- Monitoring metrics per saga instance.

## Required Output Sections

| Section | Required |
|---|---|
| Step-by-step saga flow | Yes |
| Event map | Yes |
| Risk analysis (evidence-based) | Yes |
| Failure scenarios | Yes |
| Rollback strategy | Yes |
| Isolation boundaries | Yes |
| Scaling considerations | Yes |
| Observability impact | Yes |

If any section is not applicable, mark: "Not applicable in current scope".