---
trigger: always_on
---

# Event & Messaging Rules — Intern Management Backend

## Event Naming

1. Event naming format: `<domain>.<entity>.<action>`.
2. Examples:
   - `intern.profile.created`
   - `intern.task.assigned`
   - `intern.review.submitted`

## Event Payload

3. Event payload must include:
   - `version` — schema version.
   - `timestamp` — ISO 8601 format.
   - `correlationId` — request trace identifier.
   - `data` — event-specific payload.

## Event Integrity

4. Events must be published only after transaction commit.
5. Consumers must be idempotent — duplicate processing must not cause side effects.
6. No event schema change without version bump.

## Application Events (Spring)

7. Use Spring `ApplicationEventPublisher` for intra-service domain events.
8. Event listeners must be async (`@Async`) for non-critical operations (e.g., notifications, logging).
9. Critical event handlers must handle failures gracefully with retry or dead-letter mechanism.

## AOP / Aspect Events

10. Cross-cutting event logging handled via `aspect/` package.
11. Aspects must not contain business logic — logging and auditing only.