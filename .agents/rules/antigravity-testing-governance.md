---
trigger: always_on
---

# Testing Governance — Intern Management Backend

## 1. Compliance Requirements
- **Test Coverage**: Minimum 70% branch coverage for all `service/impl/` classes.
- **Test Types**: 
    - Unit Tests (Junit 5 + Mockito)
    - Integration Tests (SpringBootTest + Testcontainers or H2)
- **naming**: `[ClassName]Test.java`

## 2. Unit Testing Patterns
- Use `@ExtendWith(MockitoExtension.class)`.
- Mock all repository and external service dependencies.
- Test both "Happy Path" and "Edge/Error Cases".
- Assertions should use `org.junit.jupiter.api.Assertions` or `org.assertj.core.api.Assertions`.

## 3. Integration Testing Patterns
- Use `@SpringBootTest`.
- Use a dedicated `application-test.yml` profile.
- Database must be cleaned between tests or use `@Transactional`.
- Mock external APIs (e.g., Mail, Payment) using `MockRestServiceServer`.

## 4. Coverage Gate
- CI/CD must fail if coverage drops below the 70% threshold.
- No new features allowed without corresponding tests.
