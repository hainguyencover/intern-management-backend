# MapStruct Governance Rule

Objective: Standardize DTO ↔ Entity mapping and eliminate manual mapping logic.

Scope: Applies to all backend services.

Rules:

1. Mandatory Usage
- All DTO ↔ Entity mappings must use MapStruct.
- Manual mapping inside Service classes is prohibited.
- No setter-based field-by-field mapping in Service layer.

2. Mapper Structure
- Location: `com.example.backend.mapper`
- Naming: `{EntityName}Mapper`
- Must use `@Mapper(componentModel = "spring")`

3. Mapping Strategy
- Use `@Mapping` for explicit field mapping.
- Use `unmappedTargetPolicy = ReportingPolicy.ERROR`.
- Nested mapping must be delegated to other mappers.

4. Update Mapping
- For update operations, use:
  `void updateEntityFromDto(Dto dto, @MappingTarget Entity entity);`

5. Validation
- Build must fail if:
  - Manual mapping is detected.
  - Mapper not registered as Spring bean.
  - Unmapped field exists.

6. Performance
- No reflection-based mapping libraries allowed.
- Only compile-time mapping permitted.

Definition of Done:
- Service contains zero mapping logic.
- All DTO conversions delegated to Mapper.