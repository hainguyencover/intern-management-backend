---
trigger: always_on
---

All actions must follow strict production governance.

1. Priority Discipline
- Tasks must be executed strictly by P-level: P0 → P1 → P2.
- No P1/P2 task is allowed before all P0 tasks are closed.

2. Definition of Done (DoD)
A task is considered DONE only when:
- Code implemented
- Test written
- Test passing
- No critical Sonar warnings
- Documented in CHANGELOG

3. Security First Policy
Any P0 security issue:
- Must be fixed before any feature development
- Requires regression test
- Requires configuration verification

4. Configuration Isolation
- No hardcoded secrets
- No production config inside dev profile
- All secrets must come from environment variables

5. API Governance
- All controllers must return ApiResponse<T>
- All endpoints must be versioned (/api/v1/)
- No permitAll() unless explicitly documented

6. Migration Discipline
- Flyway naming must follow V{number}__{description}.sql
- validate-on-migrate must be true in production
- ddl-auto must be validate or none in production

7. Testing Minimum Gate
- No merge allowed with 0 tests
- Minimum 60% coverage for services
- Critical flows (auth, intern CRUD) must have integration tests

8. DevOps Enforcement
- Dockerfile required
- docker-compose required
- .env.example required
- CI/CD pipeline required