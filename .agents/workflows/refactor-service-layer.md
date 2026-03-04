---
description: /refactor-service-layer
---

Objective:
Refactor a monolithic Service into Interface + Impl + Mapper without breaking existing behavior.

Step 1 — Freeze Behavior
- Identify all public methods.
- Note parameters and return types.

Step 2 — Extract Interface
- Create `{Domain}Service` interface in `com.example.backend.service`.
- Move all public methods to interface.
- Update controller/other services to depend on the interface (though usually they already do if it's a Spring Bean).

Step 3 — Create Implementation
- Rename original service to `{Domain}ServiceImpl` and move to `com.example.backend.service.impl`.
- Implement the newly created interface.
- Ensure `@Service` and `@Transactional` are only on the Impl.

Step 4 — Introduce MapStruct
- Create `{Domain}Mapper` in `com.example.backend.mapper`.
- Identify all manual mapping code (Entity -> DTO, DTO -> Entity).
- Replace with MapStruct mapping.
- Inject Mapper into ServiceImpl via constructor.

Step 5 — Verify Stability
- Ensure the project compiles.
- Run a build to verify MapStruct generation.
- Check that all `@PreAuthorize` and `@Transactional` rules are correctly placed.

Output:
- Service Name
- Lines Reduced (Approx)
- Mapper Created (Yes/No)
- Interface/Impl split (Yes/No)