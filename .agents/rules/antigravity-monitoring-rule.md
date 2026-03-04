---
trigger: always_on
---

# Infrastructure & Monitoring Governance — Intern Management

## 1. Containerization Standards
- **Multi-stage Builds**: Always use multi-stage Dockerfiles.
    - Stage 1: Build (Maven/NPM)
    - Stage 2: Runtime (distroless or alpine)
- **Image Naming**: `ag-[service-name]:[version]-[commit-hash]`
- **No Root**: Run containers as non-root users.

## 2. Environment Management
- No secrets in `application.yml`.
- All environment variables must have defaults for local dev.
- `.env.example` is the source of truth for required variables.

## 3. Monitoring & Logging
- **Log Format**: JSON format in production.
- **Log Levels**: 
    - `INFO` for standard operations.
    - `DEBUG` for dev/staging only.
    - `ERROR` with full stack traces.
- **Metrics**: Expose Prometheus endpoints via `/actuator/prometheus`.
- **Tracing**: Correlation IDs must be passed through headers to frontend and logs.
