
# User Story: Đăng ký & Nộp hồ sơ thực tập

## Mô tả
Là **thực tập sinh**, tôi muốn **đăng ký tài khoản và nộp hồ sơ trực tuyến** để tham gia chương trình thực tập.

---

## 0. Phạm vi MVP
- Đăng ký tài khoản
- Đăng nhập (JWT)
- Tạo / cập nhật hồ sơ cá nhân (Intern Profile)
- Upload tài liệu (CV, đơn)
- Nộp hồ sơ ứng tuyển và theo dõi trạng thái

---

## 1. Backend Tasks (Spring Boot)

### 1.1 Database & Migration (Flyway)
- BE-DB-01: Tạo bảng `users`, `roles`, `user_roles`
- BE-DB-02: Tạo bảng `intern_profiles`
- BE-DB-03: Tạo bảng `applications`
- BE-DB-04: Tạo bảng `intern_documents`
- BE-DB-05: Seed role: INTERN, HR, MENTOR, ADMIN

### 1.2 Authentication & Authorization
- BE-AUTH-01: API đăng ký `POST /api/auth/register`
- BE-AUTH-02: API đăng nhập `POST /api/auth/login`
- BE-AUTH-03: JWT + Spring Security + RBAC

### 1.3 Intern Profile
- BE-PROFILE-01: Entity/Repository/Service InternProfile
- BE-PROFILE-02: API:
  - `GET /api/me/profile`
  - `PUT /api/me/profile`
- BE-PROFILE-03: Validate dữ liệu & Global Exception Handler

### 1.4 Upload tài liệu
- BE-DOC-01: Lưu file (local/cloud)
- BE-DOC-02: API upload `POST /api/me/documents`
- BE-DOC-03: API list/delete document
- BE-DOC-04: Validate file (size, extension)

### 1.5 Application (Nộp hồ sơ)
- BE-APP-01: Entity/Service Application
- BE-APP-02: API:
  - `POST /api/me/applications`
  - `POST /api/me/applications/{id}/submit`
  - `GET /api/me/applications`
- BE-APP-03: Rule: chỉ submit khi đủ profile + CV

### 1.6 Test & Docs
- BE-TEST-01: Unit Test Service
- BE-TEST-02: Integration Test Controller
- BE-DOCS-01: Swagger / OpenAPI

---

## 2. Frontend Tasks (React + Tailwind)

### 2.1 Auth & Routing
- FE-AUTH-01: Trang Register
- FE-AUTH-02: Trang Login
- FE-AUTH-03: AuthContext + Axios Interceptor
- FE-AUTH-04: ProtectedRoute theo role INTERN

### 2.2 Hồ sơ cá nhân
- FE-PROFILE-01: Trang MyProfile (view/edit)
- FE-PROFILE-02: Validate + Toast notification

### 2.3 Upload tài liệu
- FE-DOC-01: Component DocumentUploader
- FE-DOC-02: List / Upload / Delete document

### 2.4 Nộp hồ sơ
- FE-APP-01: Trang MyApplication
- FE-APP-02: Hiển thị trạng thái (DRAFT / SUBMITTED)
- FE-APP-03: Disable submit khi chưa đủ điều kiện

### 2.5 Manual Test
- FE-TEST-01: Full flow test từ Register → Submit

---

## 3. Gợi ý chia Ticket
1. Auth & RBAC
2. Intern Profile
3. Document Upload
4. Application Submit Flow
