---
description: /api-standardization
---

Objective:
Unify API response structure.

Steps:
1. Extend ApiResponse with:
   - timestamp
   - path
2. Refactor GlobalExceptionHandler.
3. Refactor all controllers.
4. Add /api/v1 prefix.
5. Run regression test.

Output:
- ApiResponse schema
- Controllers updated count
- Exception mapping matrix