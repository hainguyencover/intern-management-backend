# Service Interface / Implementation Separation Rule

Objective: Standardize service layer structure.

Structure:
```
service/
  ├── InternService.java
  ├── AuthService.java
  └── impl/
        ├── InternServiceImpl.java
        └── AuthServiceImpl.java
```

Rules:

1. Naming Convention
- Interface: `{Domain}Service`
- Implementation: `{Domain}ServiceImpl`

2. Responsibilities
Interface:
- Define contract only.
- No annotations except JavaDoc.

Implementation:
- Annotated with `@Service`.
- Contains business logic.
- Contains `@Transactional`.

3. Dependency Injection
- Controllers depend only on Interface.
- No controller imports Impl class.

4. Visibility
- `impl` package must not be used outside service layer.

5. Single Responsibility
- Service must not exceed 250 lines.
- If exceeded -> split into domain-specific services.

6. Constructor Injection
- Field injection prohibited.

Definition of Done:
- All services follow interface/impl pattern.
- No direct injection of Impl in Controller.