---
description: /production-config-setup
---

Objective:
Split configuration into dev and prod profiles.

Steps:
1. Create application.yml
2. Create application-dev.yml
3. Create application-prod.yml
4. Configure:
   - ddl-auto
   - logging level
   - flyway validate
   - swagger enable/disable
5. Validate active profile resolution.

Output:
- Profile matrix
- Property comparison table
- Validation checklist