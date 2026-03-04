---
description: /security-hardening
---

Objective:
Eliminate all P0 security risks.

Steps:
1. Scan configuration for hardcoded secrets.
2. Move secrets to environment variables.
3. Verify JWT configuration (secret, expiration, refresh strategy).
4. Validate CORS configuration against whitelist.
5. Disable Swagger in production profile.
6. Remove permitAll() exposure.
7. Enable rate limiting.

Output Format:
- Issue
- Risk Level
- Fix Applied
- Verification Method
- Residual Risk