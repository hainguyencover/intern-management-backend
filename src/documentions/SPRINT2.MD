# SPRINT 2: QUẢN LÝ HỒ SƠ & TUYỂN DỤNG (Tuần 3-4)

## 📋 TỔNG QUAN SPRINT
**Thời gian:** 14 ngày (Tuần 3-4)  
**Mục tiêu:** Hoàn thành quy trình tuyển dụng, quản lý hồ sơ và tài liệu  
**Điểm Story:** ~60 điểm  
**User Stories:** #1-10, 29-30

## 👥 PHÂN CÔNG
| Người | Vai trò | Công việc chính |
|-------|---------|-----------------|
| BE1 | Backend 1 | Intern Profile, Search APIs |
| BE2 | Backend 2 | Application, Contract APIs |
| BE3 | Backend 3 | Email Service, File Management |
| FE | Frontend | HR & Intern Dashboards |
| DevOps | DevOps | File Storage, Email Config |

## 🗓️ KẾ HOẠCH CHI TIẾT

### **PHASE 1: INTERN PROFILE MANAGEMENT (Ngày 15-17)**

#### **Task 1.1: Intern Profile APIs (BE1)**
- **Thời gian:** 2 ngày
- **Mô tả:** CRUD APIs cho intern profile
- **Chi tiết:**
  - [ ] API: `POST /api/interns/profiles` - Tạo profile
  - [ ] API: `PUT /api/interns/profiles/{id}` - Cập nhật profile
  - [ ] API: `GET /api/interns/profiles/{id}` - Lấy profile
  - [ ] API: `DELETE /api/interns/profiles/{id}` - Xóa profile
  - [ ] Validation: GPA, student code format

#### **Task 1.2: Advanced Search & Filter (BE1)**
- **Thời gian:** 1.5 ngày
- **Mô tả:** Tìm kiếm và lọc intern theo nhiều tiêu chí
- **Chi tiết:**
  - [ ] API: `GET /api/interns/search` với query params
  - [ ] Filter: university, major, GPA range, status
  - [ ] Pagination: page, size, sort
  - [ ] Full-text search trên name, student code

#### **Task 1.3: Intern Profile Frontend (FE)**
- **Thời gian:** 2.5 ngày
- **Mô tả:** Trang quản lý profile cho intern
- **Chi tiết:**
  - [ ] Profile form với validation
  - [ ] Upload CV component
  - [ ] Profile preview page
  - [ ] Responsive design

### **PHASE 2: APPLICATION PROCESS (Ngày 18-20)**

#### **Task 2.1: Application APIs (BE2)**
- **Thời gian:** 2 ngày
- **Mô tả:** APIs cho quy trình nộp đơn thực tập
- **Chi tiết:**
  - [ ] API: `POST /api/applications` - Nộp đơn
  - [ ] API: `GET /api/applications/{id}` - Lấy thông tin đơn
  - [ ] API: `PUT /api/applications/{id}/status` - Cập nhật trạng thái
  - [ ] Business logic: Kiểm tra điều kiện nộp đơn

#### **Task 2.2: Application Review System (BE2)**
- **Thời gian:** 1.5 ngày
- **Mô tả:** Hệ thống duyệt đơn cho HR
- **Chi tiết:**
  - [ ] API: `GET /api/applications/pending` - Danh sách đơn chờ duyệt
  - [ ] API: `POST /api/applications/{id}/review` - Duyệt/từ chối
  - [ ] Validation: HR role required
  - [ ] Audit log cho các thao tác duyệt đơn

#### **Task 2.3: Application Frontend (FE)**
- **Thời gian:** 2.5 ngày
- **Mô tả:** Trang nộp đơn và duyệt đơn
- **Chi tiết:**
  - [ ] Application form cho intern
  - [ ] Application list cho HR
  - [ ] Review modal với accept/reject
  - [ ] Application status tracking

### **PHASE 3: DOCUMENT MANAGEMENT (Ngày 21-23)**

#### **Task 3.1: Document Upload & Management (BE3)**
- **Thời gian:** 2 ngày
- **Mô tả:** Hệ thống upload và quản lý tài liệu
- **Chi tiết:**
  - [ ] API: `POST /api/documents/upload` - Upload file
  - [ ] API: `GET /api/documents/{id}` - Lấy file
  - [ ] API: `PUT /api/documents/{id}/verify` - Xác thực file
  - [ ] Support: CV, transcript, application letter
  - [ ] File validation: type, size, virus scan

#### **Task 3.2: Document Verification Workflow (BE3)**
- **Thời gian:** 1.5 ngày
- **Mô tả:** Quy trình xác thực tài liệu
- **Chi tiết:**
  - [ ] API: `GET /api/documents/pending` - Tài liệu chờ xác thực
  - [ ] API: `POST /api/documents/{id}/verify` - Xác thực/từ chối
  - [ ] Email notification khi document được verify
  - [ ] Document history tracking

#### **Task 3.3: Document Frontend (FE)**
- **Thời gian:** 2.5 ngày
- **Mô tả:** UI cho upload và review documents
- **Chi tiết:**
  - [ ] File upload component với drag-drop
  - [ ] Document preview (PDF, image)
  - [ ] Verification interface cho HR
  - [ ] Document status indicators

### **PHASE 4: CONTRACT MANAGEMENT (Ngày 24-25)**

#### **Task 4.1: Contract APIs (BE2)**
- **Thời gian:** 1.5 ngày
- **Mô tả:** APIs quản lý hợp đồng thực tập
- **Chi tiết:**
  - [ ] API: `POST /api/contracts` - Tạo hợp đồng
  - [ ] API: `GET /api/contracts/{id}` - Lấy hợp đồng
  - [ ] API: `PUT /api/contracts/{id}/sign` - Ký hợp đồng
  - [ ] Contract template system

#### **Task 4.2: Digital Signature (BE3)**
- **Thời gian:** 1.5 ngày
- **Mô tả:** Hệ thống ký số cho hợp đồng
- **Chi tiết:**
  - [ ] API: `POST /api/contracts/{id}/signature` - Thêm chữ ký
  - [ ] Signature verification
  - [ ] Contract status tracking
  - [ ] Email confirmation sau khi ký

#### **Task 4.3: Contract Frontend (FE)**
- **Thời gian:** 1.5 ngày
- **Mô tả:** UI cho quản lý hợp đồng
- **Chi tiết:**
  - [ ] Contract creation form cho HR
  - [ ] Contract preview cho intern
  - [ ] Digital signature interface
  - [ ] Contract status dashboard

### **PHASE 5: EMAIL & NOTIFICATION (Ngày 26-27)**

#### **Task 5.1: Email Service Integration (BE3)**
- **Thời gian:** 1.5 ngày
- **Mô tả:** Tích hợp email service
- **Chi tiết:**
  - [ ] Setup Spring Mail với SMTP
  - [ ] Email templates (Thymeleaf)
  - [ ] Email queuing system
  - [ ] Email delivery tracking

#### **Task 5.2: Notification Events (BE3)**
- **Thời gian:** 1.5 ngày
- **Mô tả:** Event-driven notifications
- **Chi tiết:**
  - [ ] Application status change notification
  - [ ] Document verification notification
  - [ ] Contract signing notification
  - [ ] @EventListener implementation

### **PHASE 6: TESTING & OPTIMIZATION (Ngày 28)**

#### **Task 6.1: Integration Testing (All)**
- **Thời gian:** 1 ngày
- **Mô tả:** Test toàn bộ workflow
- **Chi tiết:**
  - [ ] Test application submission flow
  - [ ] Test document upload and verification
  - [ ] Test contract signing process
  - [ ] End-to-end testing với Cypress

#### **Task 6.2: Performance Optimization (DevOps)**
- **Thời gian:** 1 ngày
- **Mô tả:** Tối ưu hiệu năng
- **Chi tiết:**
  - [ ] Database query optimization
  - [ ] File upload chunking
  - [ ] Pagination optimization
  - [ ] Cache implementation với Redis

## ✅ DEFINITION OF DONE
- [ ] Intern có thể tạo và cập nhật profile
- [ ] Intern có thể nộp đơn xin thực tập online
- [ ] HR có thể duyệt/từ chối đơn và tài liệu
- [ ] Hệ thống upload và quản lý tài liệu hoạt động
- [ ] Digital contract signing hoạt động
- [ ] Email notifications gửi đúng trigger
- [ ] Search và filter intern hoạt động tốt

## 🎯 DELIVERABLES CUỐI SPRINT
1. ✅ Intern profile management system
2. ✅ Online application submission system
3. ✅ Document upload and verification
4. ✅ Digital contract management
5. ✅ Email notification system
6. ✅ Advanced search and filtering
7. ✅ HR dashboard for application review

## 🔄 WORKFLOWS HOÀN THÀNH
1. **Intern Registration Workflow:**
   - Đăng ký tài khoản → Hoàn thiện profile → Upload CV → Nộp đơn
   
2. **HR Review Workflow:**
   - Xem danh sách applications → Review documents → Duyệt đơn → Tạo hợp đồng

3. **Document Management Workflow:**
   - Upload document → Pending verification → HR review → Approved/Rejected

---
**Sprint Review:** Demo full recruitment workflow  
**Next Sprint Focus:** Internship program management và task assignment