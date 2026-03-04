---
trigger: always_on
---

# CI/CD Rules — Intern Management Backend

## Branch Strategy

1. `main` → production.
2. `develop` → staging.
3. `feature/*` → feature branches.
4. `hotfix/*` → production hotfixes.

## Pull Request Standards

5. No direct push to `main`.
6. PR must include: context, risk assessment, rollback plan.
7. PR must pass all CI checks before merge.
8. Code review required — minimum 1 approval.

## Pipeline Requirements

9. Every push triggers: lint → build → test.
10. CI must fail if test coverage < 70%.
11. Every deployment must be traceable via commit hash.
12. Automated security scanning required (dependency check).

## Commit Convention

13. Format: `<type>: <description>`
14. Types: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `perf`.
15. No ambiguous commit messages (e.g., "fix bug", "update code").

## Deployment

16. Build produces Docker image tagged with version + commit hash.
17. Staging deployment automated on `develop` merge.
18. Production deployment requires manual approval gate.