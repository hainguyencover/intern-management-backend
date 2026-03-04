---
description: /multi-tenant-audit
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
- Risk analysis must be evidence-based.
- Terminate output immediately after completion.

---

## Audit Category 1 — Tenant Isolation

- `tenant_id` presence in all business tables.
- `tenant_id` included in JWT claims.
- Tenant filter enforced at repository layer.
- No global queries without tenant filter.

## Audit Category 2 — Cross-Tenant Risk

- No shared cache keys without tenant prefix.
- No cross-tenant event leakage.
- No cross-tenant data exposure via API responses.
- DTO layer does not leak tenant-foreign data.

## Audit Category 3 — Database Isolation

- Schema isolation strategy: shared schema with `tenant_id` column or DB per tenant.
- Justification required for chosen strategy.
- Backup isolation per tenant.
- Flyway migration compatibility with tenant strategy.

## Audit Category 4 — Security

- Rate limiting per tenant.
- Data export restriction per tenant.
- Audit logging includes `tenant_id`.
- RBAC enforcement scoped to tenant context.

## Audit Category 5 — Performance

- Tenant load distribution analysis.
- Noisy neighbor risk assessment.
- Index coverage on `tenant_id` columns.

## Required Output Format

| Section | Required |
|---|---|
| Critical isolation issues | Yes |
| Data leakage risk | Yes |
| Failure scenarios | Yes |
| Rollback strategy | Yes |
| Isolation boundaries | Yes |
| Hardening recommendations | Yes |
| Architectural improvement plan | Yes |
| Observability impact | Yes |

If any section is not applicable, mark: "Not applicable in current scope".