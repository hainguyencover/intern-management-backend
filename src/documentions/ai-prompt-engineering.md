# AI Prompt Engineering Guide
## Fullstack Project (Spring Boot + React + MySQL)
**Role:** Team Leader  
**Objective:** Ensure on-time delivery, consistent quality, and effective AI usage across the entire project lifecycle.

---

## 1. Purpose of This Document
This document defines **standardized AI prompts** and **usage rules** for the team to:
- Speed up development
- Reduce rework
- Maintain consistent architecture and coding standards
- Support analysis, documentation, and self-testing (no dedicated tester)

AI is treated as a **supporting engineer**, not a decision-maker.

---

## 2. General Rules for Using AI
### 2.1 Mandatory Rules
- Always **provide context** (tech stack, sprint, module)
- Never copy blindly — **review & refactor**
- AI output must follow **project architecture**
- No sensitive credentials in prompts
- All AI-generated code must be **committed with explanation**

### 2.2 Prompt Structure (Required)
```
Role:
Context:
Input:
Constraints:
Expected Output:
```
Failure to follow this structure = prompt rejected.

---

## 3. Prompt Standards by Project Phase

## 3.1 Business & System Analysis
**Use cases:** requirements, user stories, flows

**Prompt Template**
```
Role: Senior Business Analyst
Context: Web application using Spring Boot + React + MySQL
Input: Product backlog items below
Constraints:
- Clear, measurable requirements
- No technical implementation
Expected Output:
- Functional requirements
- Non-functional requirements
- Assumptions
```

---

## 3.2 Architecture Design
**Use cases:** system architecture, component design

**Prompt Template**
```
Role: Senior Software Architect
Context: Fullstack system with REST API
Input: Business requirements document
Constraints:
- Monolithic backend
- React SPA frontend
- MySQL database
Expected Output:
- Architecture diagram description
- Module responsibilities
- Data flow explanation
```

---

## 3.3 Backend Development (Spring Boot)
**Use cases:** entity, repository, service, controller

**Prompt Template**
```
Role: Senior Java Spring Boot Engineer
Context: Spring Boot 3, JPA, MySQL
Input: Feature description
Constraints:
- Layered architecture
- RESTful APIs
- DTO pattern
Expected Output:
- Entity
- Repository
- Service
- Controller
```

**Mandatory Checks**
- Validation annotations
- Exception handling
- Clear naming

---

## 3.4 Frontend Development (React.jsx)
**Use cases:** components, hooks, pages

**Prompt Template**
```
Role: Senior React Engineer
Context: React 18, Axios, REST API
Input: Screen description
Constraints:
- Functional components
- Reusable components
Expected Output:
- Component structure
- State management
- API integration
```

---

## 3.5 Database Design
**Use cases:** schema, indexes, relationships

**Prompt Template**
```
Role: Senior Database Architect
Context: MySQL Workbench
Input: Entity list and relationships
Constraints:
- Normalize to 3NF
- Use foreign keys
Expected Output:
- Table definitions
- Index suggestions
```

---

## 4. AI-Assisted Testing (No Tester)
### 4.1 Backend Test Prompts
```
Role: Senior QA Automation Engineer
Context: Spring Boot REST API
Input: API endpoints
Expected Output:
- Test cases
- JUnit test examples
```

### 4.2 Frontend Test Prompts
```
Role: Frontend QA Engineer
Context: React application
Input: UI flow
Expected Output:
- Manual test checklist
- Edge cases
```

---

## 5. Sprint-Based AI Usage Rules
| Sprint | Allowed AI Usage |
|------|------------------|
| Sprint 1 | Analysis, architecture, DB |
| Sprint 2 | Backend core features |
| Sprint 3 | Frontend & integration |
| Sprint 4 | Optimization, testing, docs |

---

## 6. Code Review Using AI
**Prompt**
```
Role: Senior Code Reviewer
Context: Spring Boot / React project
Input: Code snippet
Constraints:
- Security
- Performance
- Clean code
Expected Output:
- Issues found
- Improvement suggestions
```

---

## 7. Documentation Support
AI may be used to:
- Rewrite docs for clarity
- Convert notes → markdown
- Generate diagrams description

AI may NOT:
- Invent requirements
- Change business logic

---

## 8. Risk Management
| Risk | Mitigation |
|----|-----------|
| Over-reliance on AI | Mandatory human review |
| Inconsistent output | Standard prompt templates |
| Time overrun | Sprint review every 2 weeks |

---

## 9. Final Team Leader Commitment
- Enforce prompt standards
- Review AI-generated output
- Ensure sprint deadlines
- Deliver project **on time, stable, documented**

**AI is a tool. The team owns the product.**
