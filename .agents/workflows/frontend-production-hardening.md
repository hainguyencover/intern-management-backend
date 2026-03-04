---
description: /frontend-production-hardening
---

Objective:
Stabilize frontend for production.

Steps:
1. Fix env variable mismatch.
2. Add Axios 401 interceptor.
3. Add refresh token flow.
4. Implement route lazy loading.
5. Remove redundant libraries.
6. Move token storage to httpOnly cookie strategy.

Output:
- Bundle size before/after
- Security delta
- Library consolidation list