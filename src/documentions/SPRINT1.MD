# SPRINT 1: THIẾT LẬP CƠ BẢN (Tuần 1-2)

## 📋 TỔNG QUAN SPRINT
**Thời gian:** 14 ngày (Tuần 1-2)  
**Mục tiêu:** Xây dựng hệ thống xương sống, authentication, quản lý người dùng cơ bản  
**Điểm Story:** ~50 điểm

## 👥 PHÂN CÔNG
| Người | Vai trò | Công việc chính |
|-------|---------|-----------------|
| BE1 | Backend 1 | Authentication, User Management |
| BE2 | Backend 2 | Database Design, Setup |
| BE3 | Backend 3 | Base APIs, Error Handling |
| FE | Frontend | React Setup, Auth Pages |
| DevOps | DevOps | CI/CD, Deployment Setup |

## 🗓️ KẾ HOẠCH CHI TIẾT

### **PHASE 1: DATABASE & BACKEND SETUP (Ngày 1-3)**

#### **Task 1.1: Database Design & Creation (BE2)**
- **Thời gian:** 2 ngày
- **Mô tả:** Tạo toàn bộ database schema theo ERD
- **Chi tiết:**
  - [ ] Tạo file SQL migration cho tất cả bảng
  - [ ] Thêm constraint và foreign keys
  - [ ] Tạo index cho các cột thường query
  - [ ] Viết seed data cho roles (Admin, HR, Mentor, Intern)
  - [ ] Test database connection

#### **Task 1.2: Spring Boot Project Setup (BE1)**
- **Thời gian:** 1 ngày
- **Mô tả:** Khởi tạo Spring Boot project với cấu trúc chuẩn
- **Chi tiết:**
  - [ ] Tạo project Spring Boot 3.x với Maven/Gradle
  - [ ] Cấu trúc package theo domain
  - [ ] Setup Spring Data JPA, MySQL connector
  - [ ] Cấu hình application.yml/properties

### **PHASE 2: AUTHENTICATION SYSTEM (Ngày 4-7)**

#### **Task 2.1: JWT Authentication (BE1)**
- **Thời gian:** 3 ngày
- **Mô tả:** Xây dựng hệ thống đăng nhập với JWT
- **Chi tiết:**
  - [ ] Tạo JWT Utility class (generate/validate token)
  - [ ] Implement JWT Authentication Filter
  - [ ] Tạo Spring Security Configuration
  - [ ] Setup PasswordEncoder (BCrypt)
  - [ ] Tạo API: `/api/auth/login`, `/api/auth/refresh`
  - [ ] Test với Postman

#### **Task 2.2: User Management APIs (BE1 + BE3)**
- **Thời gian:** 2 ngày
- **Mô tả:** CRUD cơ bản cho users và profiles
- **Chi tiết:**
  - [ ] API: `POST /api/users` - Tạo user mới
  - [ ] API: `GET /api/users/{id}` - Lấy thông tin user
  - [ ] API: `PUT /api/users/{id}` - Cập nhật user
  - [ ] API: `GET /api/users/me` - Lấy thông tin user hiện tại
  - [ ] Validation cho các field

#### **Task 2.3: Role-based Authorization (BE1)**
- **Thời gian:** 1 ngày
- **Mô tả:** Phân quyền theo role
- **Chi tiết:**
  - [ ] Tạo annotation `@PreAuthorize` cho các role
  - [ ] Cấu hình method security
  - [ ] Test authorization với các role khác nhau

### **PHASE 3: FRONTEND SETUP (Ngày 8-10)**

#### **Task 3.1: React Project Setup (FE)**
- **Thời gian:** 2 ngày
- **Mô tả:** Khởi tạo React project với TypeScript
- **Chi tiết:**
  - [ ] Tạo project với Create React App + TypeScript
  - [ ] Cài đặt dependencies: react-router-dom, axios, react-query
  - [ ] Setup folder structure
  - [ ] Cấu hình ESLint, Prettier
  - [ ] Setup environment variables

#### **Task 3.2: Authentication Pages (FE)**
- **Thời gian:** 2 ngày
- **Mô tả:** Tạo trang đăng nhập, đăng ký
- **Chi tiết:**
  - [ ] Login page với form validation
  - [ ] Register page với role selection
  - [ ] Error handling và loading states
  - [ ] JWT token storage (localStorage)
  - [ ] Axios interceptor cho authentication

#### **Task 3.3: Layout & Routing (FE)**
- **Thời gian:** 1 ngày
- **Mô tả:** Setup routing và layout cơ bản
- **Chi tiết:**
  - [ ] PrivateRoute component cho auth protection
  - [ ] Main layout với sidebar/navigation
  - [ ] Route structure cho 4 roles
  - [ ] 404 page

### **PHASE 4: BASE SYSTEM (Ngày 11-12)**

#### **Task 4.1: Global Exception Handling (BE3)**
- **Thời gian:** 1 ngày
- **Mô tả:** Xử lý exception toàn hệ thống
- **Chi tiết:**
  - [ ] Tạo GlobalExceptionHandler
  - [ ] Custom exceptions: NotFoundException, ValidationException
  - [ ] Standard API response format
  - [ ] Error logging

#### **Task 4.2: API Documentation (BE2)**
- **Thời gian:** 1 ngày
- **Mô tả:** Tạo API documentation với Swagger
- **Chi tiết:**
  - [ ] Setup Springdoc OpenAPI
  - [ ] Add annotations cho các API
  - [ ] Test API documentation tại `/swagger-ui.html`

#### **Task 4.3: File Upload Utility (BE3)**
- **Thời gian:** 1 ngày
- **Mô tả:** Xây dựng hệ thống upload file
- **Chi tiết:**
  - [ ] Tạo FileService để xử lý upload
  - [ ] API: `POST /api/files/upload`
  - [ ] Lưu file vào thư mục `uploads/`
  - [ ] Validate file type và size

### **PHASE 5: DEPLOYMENT & TESTING (Ngày 13-14)**

#### **Task 5.1: CI/CD Setup (DevOps)**
- **Thời gian:** 1.5 ngày
- **Mô tả:** Setup deployment pipeline
- **Chi tiết:**
  - [ ] Dockerfile cho Spring Boot
  - [ ] Dockerfile cho React
  - [ ] Docker-compose cho local development
  - [ ] GitHub Actions workflow

#### **Task 5.2: Unit Testing (BE1 + BE3)**
- **Thời gian:** 1.5 ngày
- **Mô tả:** Viết unit tests cho backend
- **Chi tiết:**
  - [ ] Test cho AuthService, UserService
  - [ ] Test repository với @DataJpaTest
  - [ ] Test controller với @WebMvcTest
  - [ ] MockMvc tests cho API endpoints

#### **Task 5.3: Integration Testing (FE + DevOps)**
- **Thời gian:** 1 ngày
- **Mô tả:** Test tích hợp frontend-backend
- **Chi tiết:**
  - [ ] Test login flow end-to-end
  - [ ] Test API calls từ frontend
  - [ ] Postman collection cho toàn bộ API
  - [ ] Performance testing cơ bản

## ✅ DEFINITION OF DONE
- [ ] Database: 30+ bảng đã tạo với đầy đủ relationships
- [ ] Authentication: Đăng nhập thành công với JWT token
- [ ] Authorization: Các role được phân quyền đúng
- [ ] Frontend: 4 trang cơ bản (Login, Register, Dashboard, 404)
- [ ] APIs: 10+ API endpoints hoạt động
- [ ] Testing: Code coverage > 70% cho backend
- [ ] Documentation: API docs đầy đủ tại /swagger-ui.html

## 📊 CÔNG CỤ & CÔNG NGHỆ
| Công cụ | Mục đích |
|---------|----------|
| MySQL Workbench | Database design |
| Postman | API testing |
| Docker | Containerization |
| JUnit 5 | Unit testing |
| React Testing Library | Frontend testing |
| Swagger UI | API documentation |
| Git/GitHub | Version control |

## ⚠️ RISKS & MITIGATION
| Risk | Mitigation |
|------|-----------|
| Database design không đúng yêu cầu | Review ERD với team trước khi code |
| JWT security issues | Use secure key, short expiration |
| Frontend-backend API mismatch | Use TypeScript interfaces, Swagger |
| Time overrun | Daily standup, track progress |

## 🎯 DELIVERABLES CUỐI SPRINT
1. ✅ Database schema hoàn chỉnh
2. ✅ Authentication system với JWT
3. ✅ User management APIs
4. ✅ React frontend với auth flow
5. ✅ API documentation với Swagger
6. ✅ Basic CI/CD pipeline
7. ✅ Unit tests cho core features

---
**Sprint Review:** Ngày 14 - Demo toàn bộ hệ thống authentication
**Sprint Retrospective:** Đánh giá hiệu suất và cải tiến quy trình
