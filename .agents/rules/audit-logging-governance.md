# Audit Logging Governance Rule

## Principles

1. **Non-Intrusiveness**: Audit logging must be implemented using AOP to avoid polluting business logic.
2. **Performance**: All audit persistence must be asynchronous (`@Async`) to ensure it does not block the main transaction.
3. **Security**: Auditing of sensitive data is strictly prohibited.

## Data Rules

- **Mandatory Fields**:
    - `userId`: Who performed the action.
    - `action`: Type of operation (CREATE, UPDATE, DELETE).
    - `entityName`: The entity affected.
    - `entityId`: Unique identifier of the affected entity.
    - `timestamp`: ISO-8601 format.
    - `ipAddress`: Originating IP of the request.
- **Prohibited Data**:
    - Passwords or hash strings.
    - Secret keys or tokens.
    - Personal Identity Numbers (unless encrypted).
    - Credit card or financial details.

## Implementation Standards

- Use `@Around` or `@AfterReturning` advice for successful operations.
- For `UPDATE` actions, capture `oldValue` and `newValue` as JSON strings for traceability.
- Use a dedicated `AuditLog` entity and repository.
- Ensure the audit system handles database failure gracefully (log locally if the audit DB is down).
