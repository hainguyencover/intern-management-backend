---
trigger: always_on
---

# DevOps Rules — Intern Management Backend

## Containerization

1. Backend must have a `Dockerfile` at project root.
2. Docker image must be multi-stage build (build → runtime).
3. Image tags must include version and commit hash (e.g., `1.0.0-abc123`).
4. No `latest` tag in production deployments.

## Infrastructure

5. Docker Compose for local development environment.
6. Services: `backend`, `db` (PostgreSQL) at minimum.
7. Environment variables managed via `.env` files — never committed to repository.
8. `.env.example` must be maintained with all required variables.

## Deployment

9. No deployment without passing all tests.
10. Blue/Green or Rolling deployment strategy required.
11. Health check endpoint (`/actuator/health`) must be configured in deployment.
12. Graceful shutdown must be supported.

## Monitoring & Logging

13. Centralized logging mandatory (structured JSON format preferred).
14. Application logs must not contain sensitive data.
15. Spring Actuator endpoints exposed for monitoring: `/actuator/health`, `/actuator/metrics`.

## Backup & Recovery

16. Database backup strategy required (daily minimum).
17. Backup restoration must be tested periodically.