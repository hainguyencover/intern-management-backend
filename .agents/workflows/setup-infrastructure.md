---
description: Bootstrap the entire development or production infrastructure.
---

# /setup-infrastructure Workflow

1. **Environment Initialization**
   - Copy `.env.example` to `.env`.
   - Validate all required variables are set.

2. **Docker Orchestration**
   - Run `docker-compose build --no-cache`.
   - Run `docker-compose up -d`.
   - Wait for health checks on DB and Backend.

3. **Database Migration**
   - Verify Flyway migrations are up to date.
   - Run `mvn flyway:info`.

4. **Verification**
   - Check `/actuator/health` on backend.
   - Check frontend home page.
