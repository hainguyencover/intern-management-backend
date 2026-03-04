# Transaction Boundary Governance Rule

Objective: Define strict transaction management policy.

Scope: Applies to all Service implementations.

Rules:

1. Placement
- `@Transactional` allowed ONLY in `ServiceImpl` classes.
- Interface must not contain `@Transactional`.
- Controller layer must not contain `@Transactional`.

2. Read Operations
- Query methods must use:
  `@Transactional(readOnly = true)`

3. Write Operations
- Create/Update/Delete must use:
  `@Transactional`
- Explicit `rollbackFor` must be defined for checked exceptions when needed.

4. Nested Calls
- Internal method calls must not rely on proxy invocation.
- If required, extract to separate service.

5. Isolation & Propagation
- Default propagation REQUIRED.
- Custom propagation must be justified in documentation.

6. Exception Handling
- Business exceptions must trigger rollback.
- Checked exceptions must be reviewed for rollback impact.

Definition of Done:
- No transaction annotation outside Impl.
- All query methods explicitly readOnly.
- No transaction leak across layers.