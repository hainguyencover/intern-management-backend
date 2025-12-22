# SPRINT 3: CHƯƠNG TRÌNH & CÔNG VIỆC (Tuần 5-6)

## 📋 TỔNG QUAN SPRINT
**Thời gian:** 14 ngày (Tuần 5-6)  
**Mục tiêu:** Quản lý chương trình thực tập, công việc hàng ngày, đánh giá  
**Điểm Story:** ~70 điểm  
**User Stories:** #11-20, 31

## 👥 PHÂN CÔNG
| Người | Vai trò | Công việc chính |
|-------|---------|-----------------|
| BE1 | Backend 1 | Program Management APIs |
| BE2 | Backend 2 | Task & Evaluation APIs |
| BE3 | Backend 3 | Report & Statistics APIs |
| FE | Frontend | Program & Task UIs |
| DevOps | DevOps | Monitoring & Analytics |

## 🗓️ KẾ HOẠCH CHI TIẾT

### **PHASE 1: INTERNSHIP PROGRAM MANAGEMENT (Ngày 29-31)**

#### **Task 1.1: Program CRUD APIs (BE1)**
- **Thời gian:** 2 ngày
- **Mô tả:** APIs quản lý chương trình thực tập
- **Chi tiết:**
  - [ ] API: `POST /api/programs` - Tạo chương trình
  - [ ] API: `PUT /api/programs/{id}` - Cập nhật chương trình
  - [ ] API: `GET /api/programs` - Danh sách chương trình
  - [ ] API: `GET /api/programs/{id}` - Chi tiết chương trình
  - [ ] Validation: Date ranges, capacity

#### **Task 1.2: Program Assignment System (BE1)**
- **Thời gian:** 1.5 ngày
- **Mô tả:** Phân công intern vào chương trình
- **Chi tiết:**
  - [ ] API: `POST /api/programs/{id}/assign-intern` - Phân công intern
  - [ ] API: `POST /api/programs/{id}/assign-mentor` - Phân công mentor
  - [ ] API: `GET /api/programs/{id}/participants` - Danh sách participants
  - [ ] Business rules: Mentor capacity, program capacity

#### **Task 1.3: Program Frontend (FE)**
- **Thời gian:** 2.5 ngày
- **Mô tả:** UI quản lý chương trình
- **Chi tiết:**
  - [ ] Program creation wizard
  - [ ] Program dashboard với calendar
  - [ ] Participant management interface
  - [ ] Program timeline visualization

### **PHASE 2: TASK MANAGEMENT (Ngày 32-34)**

#### **Task 2.1: Task APIs (BE2)**
- **Thời gian:** 2 ngày
- **Mô tả:** APIs quản lý công việc
- **Chi tiết:**
  - [ ] API: `POST /api/tasks` - Tạo task
  - [ ] API: `PUT /api/tasks/{id}` - Cập nhật task
  - [ ] API: `GET /api/tasks/assigned` - Task được giao
  - [ ] API: `GET /api/tasks/assigned-to-me` - Task của tôi
  - [ ] Task status workflow: TODO → IN_PROGRESS → DONE

#### **Task 2.2: Task Progress Tracking (BE2)**
- **Thời gian:** 1.5 ngày
- **Mô tả:** Theo dõi tiến độ task
- **Chi tiết:**
  - [ ] API: `PUT /api/tasks/{id}/progress` - Cập nhật tiến độ
  - [ ] API: `POST /api/tasks/{id}/comments` - Thêm comment
  - [ ] API: `GET /api/tasks/{id}/history` - Lịch sử task
  - [ ] Automatic status updates based on progress

#### **Task 2.3: Task Frontend (FE)**
- **Thời gian:** 2.5 ngày
- **Mô tả:** UI quản lý task
- **Chi tiết:**
  - [ ] Task board (Kanban style)
  - [ ] Task creation modal
  - [ ] Progress tracking interface
  - [ ] Task comments và attachments
  - [ ] Deadline reminders

### **PHASE 3: WEEKLY REPORTS (Ngày 35-36)**

#### **Task 3.1: Report APIs (BE3)**
- **Thời gian:** 1.5 ngày
- **Mô tả:** APIs quản lý báo cáo tuần
- **Chi tiết:**
  - [ ] API: `POST /api/reports/weekly` - Nộp báo cáo
  - [ ] API: `GET /api/reports` - Danh sách báo cáo
  - [ ] API: `PUT /api/reports/{id}/feedback` - Thêm feedback
  - [ ] Report template system

#### **Task 3.2: Report Feedback System (BE3)**
- **Thời gian:** 1.5 ngày
- **Mô tả:** Hệ thống phản hồi cho báo cáo
- **Chi tiết:**
  - [ ] API: `POST /api/reports/{id}/comments` - Thêm comment
  - [ ] API: `PUT /api/reports/{id}/status` - Đánh dấu đã review
  - [ ] Notification khi report được review
  - [ ] Report rating system

#### **Task 3.3: Report Frontend (FE)**
- **Thời gian:** 2 ngày
- **Mô tả:** UI cho weekly reports
- **Chi tiết:**
  - [ ] Report submission form
  - [ ] Report list với filter theo week
  - [ ] Report review interface cho mentor
  - [ ] Rich text editor cho report content

### **PHASE 4: EVALUATION SYSTEM (Ngày 37-39)**

#### **Task 4.1: Evaluation APIs (BE2)**
- **Thời gian:** 2 ngày
- **Mô tả:** APIs đánh giá intern
- **Chi tiết:**
  - [ ] API: `POST /api/evaluations` - Tạo đánh giá
  - [ ] API: `GET /api/evaluations/intern/{id}` - Đánh giá của intern
  - [ ] API: `PUT /api/evaluations/{id}/submit` - Submit đánh giá
  - [ ] Evaluation criteria management

#### **Task 4.2: Evaluation Templates (BE2)**
- **Thời gian:** 1.5 ngày
- **Mô tả:** Hệ thống template đánh giá
- **Chi tiết:**
  - [ ] API: `GET /api/evaluation-templates` - Danh sách template
  - [ ] API: `POST /api/evaluation-templates` - Tạo template
  - [ ] Dynamic evaluation forms
  - [ ] Score calculation logic

#### **Task 4.3: Evaluation Frontend (FE)**
- **Thời gian:** 2.5 ngày
- **Mô tả:** UI cho hệ thống đánh giá
- **Chi tiết:**
  - [ ] Evaluation form dynamic rendering
  - [ ] Score summary dashboard
  - [ ] Evaluation history
  - [ ] Final grade calculation display

### **PHASE 5: MENTOR MANAGEMENT (Ngày 40-41)**

#### **Task 5.1: Mentor APIs (BE1)**
- **Thời gian:** 1.5 ngày
- **Mô tả:** APIs quản lý mentor
- **Chi tiết:**
  - [ ] API: `GET /api/mentors` - Danh sách mentor
  - [ ] API: `GET /api/mentors/{id}/workload` - Khối lượng công việc
  - [ ] API: `PUT /api/mentors/{id}/capacity` - Cập nhật capacity
  - [ ] Mentor assignment optimization

#### **Task 5.2: Mentor Dashboard APIs (BE3)**
- **Thời gian:** 1.5 ngày
- **Mô tả:** APIs cho mentor dashboard
- **Chi tiết:**
  - [ ] API: `GET /api/mentors/dashboard` - Mentor dashboard data
  - [ ] API: `GET /api/mentors/{id}/interns` - Danh sách intern
  - [ ] API: `GET /api/mentors/{id}/performance` - Performance metrics

#### **Task 5.3: Mentor Frontend (FE)**
- **Thời gian:** 2 ngày
- **Mô tả:** UI cho mentor dashboard
- **Chi tiết:**
  - [ ] Mentor dashboard với overview
  - [ ] Intern management interface
  - [ ] Workload monitoring
  - [ ] Performance analytics

### **PHASE 6: ANALYTICS & REPORTING (Ngày 42)**

#### **Task 6.1: Statistics APIs (BE3)**
- **Thời gian:** 1 ngày
- **Mô tả:** APIs thống kê và analytics
- **Chi tiết:**
  - [ ] API: `GET /api/statistics/program-completion` - Tỷ lệ hoàn thành
  - [ ] API: `GET /api/statistics/university-distribution` - Phân bổ trường
  - [ ] API: `GET /api/statistics/mentor-workload` - Khối lượng mentor
  - [ ] Real-time dashboard data

#### **Task 6.2: Report Generation (DevOps)**
- **Thời gian:** 1 ngày
- **Mô tả:** Tạo báo cáo PDF/Excel
- **Chi tiết:**
  - [ ] API: `POST /api/reports/generate` - Generate report
  - [ ] PDF generation với iText/Apache PDFBox
  - [ ] Excel export với Apache POI
  - [ ] Report scheduling

## ✅ DEFINITION OF DONE
- [ ] HR có thể tạo và quản lý chương trình thực tập
- [ ] Mentor có thể giao task và theo dõi tiến độ
- [ ] Intern có thể nộp weekly report
- [ ] Hệ thống đánh giá intern hoạt động
- [ ] Mentor workload balancing
- [ ] Program statistics và reporting
- [ ] Real-time dashboard cho mentor/HR

## 🎯 DELIVERABLES CUỐI SPRINT
1. ✅ Internship program management system
2. ✅ Task assignment and tracking system
3. ✅ Weekly report submission and review
4. ✅ Intern evaluation system
5. ✅ Mentor management dashboard
6. ✅ Program analytics và statistics
7. ✅ PDF/Excel report generation

## 🔄 WORKFLOWS HOÀN THÀNH
1. **Program Management Workflow:**
   - Create program → Assign mentors → Assign interns → Start program
   
2. **Task Management Workflow:**
   - Mentor creates task → Intern accepts → Progress updates → Completion
   
3. **Weekly Evaluation Workflow:**
   - Intern submits report → Mentor reviews → Add feedback → Track progress
   
4. **Final Evaluation Workflow:**
   - Mentor evaluates skills/attitude → Calculate final grade → Generate report

---
**Sprint Review:** Demo full internship management system  
**Next Sprint Focus:** Attendance tracking, support system, and final deployment