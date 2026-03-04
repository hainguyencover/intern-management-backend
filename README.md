# 📋 Intern Management System — Backend

> **A production-grade Spring Boot backend for managing interns, tasks, evaluations, and reports.**

---

## 🏗️ Architecture Overview

```
com.example.backend/
│
├── annotation/          # Custom annotations (e.g., @CurrentUser)
├── aspect/              # AOP aspects for audit logging & cross-cutting concerns
├── config/              # Spring configuration (Security, Cache, CORS, Mail, etc.)
├── controller/          # REST API controllers, grouped by domain
│   ├── admin/           # Admin: users, roles, backup, system config
│   ├── auth/            # Authentication & 2FA
│   ├── hr/              # HR: applications, exports, analytics
│   ├── intern/          # Intern: profile, tasks, documents, weekly reports
│   ├── mentor/          # Mentor: tasks, reviews, evaluations
│   └── common/          # Shared: programs, groups, notifications
├── dto/
│   ├── request/         # Inbound DTOs (API payloads)
│   └── response/        # Outbound DTOs
├── elasticsearch/       # Elasticsearch integration (optional)
├── entity/              # JPA entities (never exposed in API responses)
├── enums/               # Enum constants
├── exception/           # Global exception handler & custom exceptions
├── mapper/              # Entity ↔ DTO converters
├── repository/          # Spring Data JPA repositories
├── security/            # JWT, CustomUserDetails, SecurityConfig
└── service/             # Service interfaces
    └── impl/            # Service implementations
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.4.1 |
| Database | MySQL 8 |
| ORM | Spring Data JPA / Hibernate |
| Auth | JWT + Spring Security + 2FA (TOTP) |
| Migration | Flyway |
| Caching | Redis |
| Search (optional) | Elasticsearch |
| AI | OpenAI API (Spring AI) |
| Build | Gradle |
| Container | Docker / Docker Compose |
| Docs | SpringDoc OpenAPI (Swagger UI) |

---

## 🚀 Local Development Setup

### Prerequisites
- Java 17+
- MySQL 8 running on `localhost:3306`
- Redis running on `localhost:6379` *(for caching)*
- Docker *(optional, for full stack setup)*

### 1. Clone & Configure
```bash
git clone <repository-url>
cd backend
cp .env.example .env
# Edit .env and fill in your credentials
```

### 2. Database Setup
```sql
CREATE DATABASE management_intern CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Run the Application
```bash
./gradlew bootRun
```

The application will start at `http://localhost:8080`.

Flyway will automatically run all migrations in `src/main/resources/db/migration/`.

### 4. Docker Compose (Full Stack)
```bash
docker-compose up -d
```

---

## 🔌 API Documentation

Swagger UI is available at:
```
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:
```
http://localhost:8080/v3/api-docs
```

---

## 📁 API Endpoint Overview

| Prefix | Domain |
|---|---|
| `/api/v1/auth` | Login, Register, Refresh Token, 2FA |
| `/api/v1/admin` | Admin user management, roles, permissions |
| `/api/v1/interns` | Intern profile, CV, mentor assignment |
| `/api/v1/mentors` | Mentor profiles |
| `/api/v1/tasks` | Task creation, assignment, progress |
| `/api/v1/weekly-reports` | Intern weekly reports |
| `/api/v1/programs` | Internship programs |
| `/api/v1/program-groups` | Program groups |
| `/api/v1/applications` | Intern applications |
| `/api/v1/contracts` | Internship contracts |
| `/api/v1/evaluations` | Performance evaluations |
| `/api/v1/leave-requests` | Leave management |
| `/api/v1/notifications` | Notification system |
| `/api/v1/support-tickets` | Support ticket system |
| `/api/v1/search` | Elasticsearch full-text search (optional) |
| `/api/v1/ai` | AI-powered CV screening |
| `/api/v1/export` | Data export (Excel/PDF) |
| `/api/v1/backup` | System backup |
| `/api/v1/audit-logs` | Audit trail |
| `/api/v1/system-config` | System configuration |

---

## 🔐 Authentication Flow

```
POST /api/v1/auth/login
  → returns: { token, refreshToken, id, email, roles }

POST /api/v1/auth/refresh
  → returns: { token, refreshToken }

POST /api/v1/auth/2fa/setup      → Setup TOTP QR Code
POST /api/v1/auth/2fa/verify     → Enable 2FA
POST /api/v1/auth/2fa/disable    → Disable 2FA
```

All protected endpoints require:
```
Authorization: Bearer <token>
```

---

## 🌍 Environment Variables

See [.env.example](.env.example) for the complete list. Required variables:

| Variable | Description |
|---|---|
| `DB_URL` | MySQL JDBC connection URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | At least 256-bit secret for JWT signing |
| `OPENAI_API_KEY` | OpenAI key for AI features |
| `MAIL_USERNAME` | SMTP sender email |
| `MAIL_PASSWORD` | SMTP app password |

> ⚠️ **Never commit `.env` to version control.**

---

## 🗄️ Database Migrations

All schema changes are managed via Flyway:

| Version | Description |
|---|---|
| V1 | Initial schema |
| V2 | RBAC permissions |
| V3 | Seed data |
| V4 | Intern profile fields |
| V5 | RBAC + Audit + Backup tables |
| V6 | Audit columns |
| V7 | AI Insights fields |
| V8 | Fix 2FA column NULL constraint |

---

## 🔌 Optional Services

### Elasticsearch (Search)
Disabled by default. Enable with:
```env
APP_ES_ENABLED=true
ES_URIS=http://localhost:9200
```

### Redis (Caching)
Required for caching. Disable cache type in `application.yml` if Redis is unavailable:
```yaml
spring.cache.type: none
```

---

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests BackendApplicationTests
```

Test profile uses H2 in-memory database (see `src/test/resources/application-test.yml`).

---

## 🐳 Docker

### Build Image
```bash
docker build -t intern-management-backend:1.0.0 .
```

### Run with Docker Compose
```bash
docker-compose up -d
```

Services included:
- `backend` on port `8080`
- `db` (MySQL) on port `3306`

---

## 📊 Monitoring

Spring Actuator endpoints:
```
GET /actuator/health
GET /actuator/metrics
GET /actuator/prometheus
```

---

## 🌿 Git Workflow

```
main        → production
develop     → staging
feature/*   → new features
hotfix/*    → production hotfixes
```

### Commit Convention
```
feat: add intern module
fix: resolve JWT expiration bug
refactor: optimize task service
chore: update docker config
docs: update README
test: add auth integration tests
```

---

## 👥 Default Test Credentials

| Role | Email | Password |
|---|---|---|
| Admin | admin@company.com | admin123 |
| HR | hr@company.com | hr123 |
| Mentor | mentor1@company.com | mentor123 |
| Intern | intern@student.com | intern123 |

> ⚠️ Change all credentials before deploying to production.
