# User Story: HR duyệt hoặc từ chối hồ sơ để chọn ứng viên phù hợp (IMS)

> **User story:** Là HR, tôi muốn duyệt hoặc từ chối hồ sơ để chọn ứng viên phù hợp.  
> **Tech stack:** Spring Boot (Java 17+) + MySQL + React.jsx (Vite) + Tailwind CSS  
> **Auth:** JWT + RBAC (ROLE_HR)  
> **Gợi ý kiến trúc:** Monolith theo module (khuyến nghị MVP), hoặc Microservice (tuỳ giai đoạn)

---

## 0) Definition of Done (DoD)
- HR xem được danh sách hồ sơ ứng tuyển có phân trang + lọc theo trạng thái.
- HR xem được chi tiết hồ sơ + tài liệu đính kèm.
- HR có thể **Approve** hoặc **Reject** 1 hồ sơ (1 lần quyết định cuối cùng, có thể cho phép “re-review” theo rule).
- Khi duyệt/từ chối:
  - cập nhật `applications.status`
  - ghi log vào `application_reviews`
- API bảo vệ theo role HR.
- FE hiển thị danh sách + chi tiết + modal duyệt/từ chối, có toast thông báo.
- Có validate dữ liệu & xử lý lỗi thống nhất.
- Có test tối thiểu (service + controller).

---

## 1) Chia nhỏ Task (Backend + Frontend + DB + Test)

### 1.1 Backend (Monolith module `application-review`)
**B1. DB/Migration**
1. Tạo/hoàn thiện bảng:
   - `applications` (status: `DRAFT|SUBMITTED|APPROVED|REJECTED|CONTRACT_SENT|CONTRACT_SIGNED`)
   - `application_reviews` (reviewer_id, decision, comment, decided_at)
2. Thêm index:
   - `applications(status, applied_at)`
   - `application_reviews(application_id, decided_at)`

**B2. Domain/Entity + Enum**
1. Enum `ApplicationStatus`
2. Enum `ReviewDecision`
3. Entity `Application` (liên kết `intern_profiles`)
4. Entity `ApplicationReview` (liên kết `applications`, `users`)

**B3. Repository**
1. `ApplicationRepository`
2. `ApplicationReviewRepository`

**B4. DTO + Validation**
1. `ApplicationListItemResponse`
2. `ApplicationDetailResponse`
3. `ReviewRequest` (decision, comment)
4. `ReviewResponse`

**B5. Service**
1. `ApplicationQueryService`: list/filter + detail
2. `ApplicationReviewService`: approve/reject + transaction + rule kiểm tra trạng thái

**B6. Controller**
1. `GET /api/hr/applications` (paging + status + keyword)
2. `GET /api/hr/applications/{id}`
3. `POST /api/hr/applications/{id}/review`

**B7. Security (RBAC)**
- Chặn endpoint bằng `@PreAuthorize("hasRole('HR')")` hoặc cấu hình SecurityFilterChain.

**B8. Exception handling**
- `ApiError` + `GlobalExceptionHandler`:
  - 404 khi không tồn tại application
  - 400 khi status không hợp lệ (ví dụ đã APPROVED/REJECTED mà review lại)

**B9. Test**
- Unit test service (rule status + tạo review)
- WebMvc test controller (security + validation + response)

---

### 1.2 Frontend (React.jsx + Tailwind)
**F1. Routes**
- `/hr/applications` → danh sách
- `/hr/applications/:id` → chi tiết + panel duyệt/từ chối

**F2. UI Components**
1. `ApplicationStatusBadge`
2. `ReviewModal` (Approve/Reject + comment)
3. `Pagination` (hoặc dùng simple paging)

**F3. Pages**
1. `HrApplicationList.jsx`
2. `HrApplicationDetail.jsx`

**F4. API client**
- `src/api/hrApplications.js`:
  - `listApplications(params)`
  - `getApplication(id)`
  - `reviewApplication(id, payload)`

**F5. UX & Validation**
- Disable button khi đang submit
- Toast success/error
- Confirm trước khi duyệt/từ chối

**F6. FE tests (tuỳ chọn)**
- Manual checklist:
  - lọc status, chuyển trang, vào chi tiết, duyệt/từ chối, refresh thấy status cập nhật

---

### 1.3 Nếu chọn Microservice (gợi ý sau MVP)
- Service `application-service` (quản lý Application)
- Service `review-service` (quản lý ApplicationReview)
- API Gateway + JWT shared
- Event (Kafka/Rabbit) khi review xong để gửi email/notify

---

## 2) DB Migration mẫu (Flyway)

> File: `src/main/resources/db/migration/V3__application_review.sql`

```sql
-- Applications
CREATE TABLE IF NOT EXISTS applications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  intern_id BIGINT NOT NULL,
  position VARCHAR(255),
  applied_at DATETIME NOT NULL,
  status VARCHAR(50) NOT NULL,
  note TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_app_intern FOREIGN KEY (intern_id) REFERENCES intern_profiles(id)
);

CREATE INDEX idx_app_status_applied ON applications(status, applied_at);

-- Reviews
CREATE TABLE IF NOT EXISTS application_reviews (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  application_id BIGINT NOT NULL,
  reviewer_id BIGINT NOT NULL,
  decision VARCHAR(50) NOT NULL,
  comment TEXT,
  decided_at DATETIME NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_review_app FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE,
  CONSTRAINT fk_review_user FOREIGN KEY (reviewer_id) REFERENCES users(id)
);

CREATE INDEX idx_review_app_decided ON application_reviews(application_id, decided_at);
```

---

## 3) Backend Code (Spring Boot) — Full mẫu

### 3.1 Enums

```java
package com.ims.application.domain;

public enum ApplicationStatus {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED,
    CONTRACT_SENT,
    CONTRACT_SIGNED
}
```

```java
package com.ims.application.domain;

public enum ReviewDecision {
    APPROVE,
    REJECT
}
```

---

### 3.2 Entities

```java
package com.ims.application.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "intern_id", nullable = false)
    private Long internId;

    @Column(name = "position")
    private String position;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ApplicationStatus status;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = this.createdAt == null ? LocalDateTime.now() : this.createdAt;
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // getters/setters
    public Long getId() { return id; }
    public Long getInternId() { return internId; }
    public void setInternId(Long internId) { this.internId = internId; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
```

```java
package com.ims.application.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "application_reviews")
public class ApplicationReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 50)
    private ReviewDecision decision;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "decided_at", nullable = false)
    private LocalDateTime decidedAt;

    @PrePersist
    void prePersist() {
        this.decidedAt = this.decidedAt == null ? LocalDateTime.now() : this.decidedAt;
    }

    // getters/setters
    public Long getId() { return id; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public Long getReviewerId() { return reviewerId; }
    public void setReviewerId(Long reviewerId) { this.reviewerId = reviewerId; }
    public ReviewDecision getDecision() { return decision; }
    public void setDecision(ReviewDecision decision) { this.decision = decision; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
}
```

---

### 3.3 Repositories

```java
package com.ims.application.repository;

import com.ims.application.domain.Application;
import com.ims.application.domain.ApplicationStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    @Query("SELECT a FROM Application a " +
           "WHERE (:status IS NULL OR a.status = :status) " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(a.position) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY a.appliedAt DESC")
    Page<Application> search(@Param("status") ApplicationStatus status,
                            @Param("keyword") String keyword,
                            Pageable pageable);
}
```

```java
package com.ims.application.repository;

import com.ims.application.domain.ApplicationReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationReviewRepository extends JpaRepository<ApplicationReview, Long> {
    boolean existsByApplicationId(Long applicationId);
}
```

---

### 3.4 DTOs

```java
package com.ims.application.dto;

import com.ims.application.domain.ApplicationStatus;
import java.time.LocalDateTime;

public record ApplicationListItemResponse(
        Long id,
        Long internId,
        String position,
        LocalDateTime appliedAt,
        ApplicationStatus status
) {}
```

```java
package com.ims.application.dto;

import com.ims.application.domain.ApplicationStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ApplicationDetailResponse(
        Long id,
        Long internId,
        String position,
        LocalDateTime appliedAt,
        ApplicationStatus status,
        String note,
        List<ReviewResponse> reviews
) {}
```

```java
package com.ims.application.dto;

import com.ims.application.domain.ReviewDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @NotNull ReviewDecision decision,
        @Size(max = 2000) String comment
) {}
```

```java
package com.ims.application.dto;

import com.ims.application.domain.ReviewDecision;
import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long reviewerId,
        ReviewDecision decision,
        String comment,
        LocalDateTime decidedAt
) {}
```

---

### 3.5 Exceptions + Error response

```java
package com.ims.common.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}
```

```java
package com.ims.common.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}
```

```java
package com.ims.common.exception;

import java.time.Instant;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {}
```

```java
package com.ims.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ApiError(Instant.now(), 404, "NOT_FOUND", ex.getMessage(), req.getRequestURI())
        );
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> badRequest(BadRequestException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiError(Instant.now(), 400, "BAD_REQUEST", ex.getMessage(), req.getRequestURI())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("Validation error");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                new ApiError(Instant.now(), 400, "VALIDATION_ERROR", msg, req.getRequestURI())
        );
    }
}
```

---

### 3.6 Services

```java
package com.ims.application.service;

import com.ims.application.domain.Application;
import com.ims.application.domain.ApplicationStatus;
import com.ims.application.dto.*;
import com.ims.application.repository.ApplicationRepository;
import com.ims.application.repository.ApplicationReviewRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApplicationQueryService {

    private final ApplicationRepository applicationRepo;
    private final ApplicationReviewRepository reviewRepo;

    public ApplicationQueryService(ApplicationRepository applicationRepo, ApplicationReviewRepository reviewRepo) {
        this.applicationRepo = applicationRepo;
        this.reviewRepo = reviewRepo;
    }

    public Page<ApplicationListItemResponse> list(ApplicationStatus status, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return applicationRepo.search(status, keyword, pageable)
                .map(a -> new ApplicationListItemResponse(a.getId(), a.getInternId(), a.getPosition(), a.getAppliedAt(), a.getStatus()));
    }

    public Application getOrThrow(Long id) {
        return applicationRepo.findById(id)
                .orElseThrow(() -> new com.ims.common.exception.NotFoundException("Application not found: " + id));
    }

    public ApplicationDetailResponse detail(Long id, List<ReviewResponse> reviews) {
        Application a = getOrThrow(id);
        return new ApplicationDetailResponse(
                a.getId(),
                a.getInternId(),
                a.getPosition(),
                a.getAppliedAt(),
                a.getStatus(),
                a.getNote(),
                reviews
        );
    }
}
```

```java
package com.ims.application.service;

import com.ims.application.domain.*;
import com.ims.application.dto.*;
import com.ims.application.repository.*;
import com.ims.common.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApplicationReviewService {

    private final ApplicationRepository applicationRepo;
    private final ApplicationReviewRepository reviewRepo;

    public ApplicationReviewService(ApplicationRepository applicationRepo, ApplicationReviewRepository reviewRepo) {
        this.applicationRepo = applicationRepo;
        this.reviewRepo = reviewRepo;
    }

    @Transactional
    public ReviewResponse review(Long applicationId, Long reviewerId, ReviewRequest req) {
        Application app = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new com.ims.common.exception.NotFoundException("Application not found: " + applicationId));

        // Rule: chỉ review khi SUBMITTED
        if (app.getStatus() != ApplicationStatus.SUBMITTED) {
            throw new BadRequestException("Only SUBMITTED applications can be reviewed. Current status=" + app.getStatus());
        }

        // Rule: mỗi application chỉ có 1 quyết định cuối (tuỳ yêu cầu có thể bỏ)
        if (reviewRepo.existsByApplicationId(applicationId)) {
            throw new BadRequestException("Application already reviewed.");
        }

        ApplicationReview review = new ApplicationReview();
        review.setApplicationId(applicationId);
        review.setReviewerId(reviewerId);
        review.setDecision(req.decision());
        review.setComment(req.comment());
        review.setDecidedAt(LocalDateTime.now());

        review = reviewRepo.save(review);

        // cập nhật status
        if (req.decision() == ReviewDecision.APPROVE) {
            app.setStatus(ApplicationStatus.APPROVED);
        } else {
            app.setStatus(ApplicationStatus.REJECTED);
        }
        applicationRepo.save(app);

        return new ReviewResponse(review.getId(), review.getReviewerId(), review.getDecision(), review.getComment(), review.getDecidedAt());
    }

    public List<ReviewResponse> listReviews(Long applicationId) {
        // MVP: rule chỉ 1 review → có thể trả List.of()
        // Nếu muốn history: query repo rồi map sang ReviewResponse
        return List.of();
    }
}
```

---

### 3.7 Controller

```java
package com.ims.application.controller;

import com.ims.application.domain.ApplicationStatus;
import com.ims.application.dto.*;
import com.ims.application.service.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr/applications")
@PreAuthorize("hasRole('HR')")
public class HrApplicationController {

    private final ApplicationQueryService queryService;
    private final ApplicationReviewService reviewService;

    public HrApplicationController(ApplicationQueryService queryService, ApplicationReviewService reviewService) {
        this.queryService = queryService;
        this.reviewService = reviewService;
    }

    @GetMapping
    public Page<ApplicationListItemResponse> list(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return queryService.list(status, keyword, page, size);
    }

    @GetMapping("/{id}")
    public ApplicationDetailResponse detail(@PathVariable Long id) {
        List<ReviewResponse> reviews = reviewService.listReviews(id);
        return queryService.detail(id, reviews);
    }

    @PostMapping("/{id}/review")
    public ReviewResponse review(@PathVariable Long id, @Valid @RequestBody ReviewRequest req, Authentication auth) {
        // Tuỳ hệ thống: lấy userId từ JWT principal.
        // Ví dụ: ((CustomUserDetails) auth.getPrincipal()).getId()
        Long reviewerId = 1L; // TODO: sửa theo dự án của bạn
        return reviewService.review(id, reviewerId, req);
    }
}
```

---

## 4) Frontend Code (React.jsx + Tailwind) — Full mẫu

### 4.1 API client

> File: `src/api/hrApplications.js`

```jsx
import axiosClient from "@/api/axiosClient";

export async function listApplications({ status, keyword, page = 0, size = 10 }) {
  const params = { page, size };
  if (status) params.status = status;
  if (keyword) params.keyword = keyword;
  const { data } = await axiosClient.get("/hr/applications", { params });
  return data;
}

export async function getApplication(id) {
  const { data } = await axiosClient.get(`/hr/applications/${id}`);
  return data;
}

export async function reviewApplication(id, payload) {
  const { data } = await axiosClient.post(`/hr/applications/${id}/review`, payload);
  return data;
}
```

---

### 4.2 Status badge

> File: `src/components/ApplicationStatusBadge.jsx`

```jsx
import React from "react";

const map = {
  DRAFT: "bg-slate-100 text-slate-700 border-slate-200",
  SUBMITTED: "bg-blue-50 text-blue-700 border-blue-200",
  APPROVED: "bg-emerald-50 text-emerald-700 border-emerald-200",
  REJECTED: "bg-rose-50 text-rose-700 border-rose-200",
  CONTRACT_SENT: "bg-amber-50 text-amber-700 border-amber-200",
  CONTRACT_SIGNED: "bg-purple-50 text-purple-700 border-purple-200",
};

export default function ApplicationStatusBadge({ status }) {
  const cls = map[status] || "bg-slate-100 text-slate-700 border-slate-200";
  return (
    <span className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold ${cls}`}>
      {status}
    </span>
  );
}
```

---

### 4.3 Review modal

> File: `src/components/ReviewModal.jsx`

```jsx
import React, { useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { toast } from "sonner";

export default function ReviewModal({ open, onClose, onSubmit, mode = "APPROVE" }) {
  const [comment, setComment] = useState("");
  const [loading, setLoading] = useState(false);

  const title = useMemo(() => (mode === "APPROVE" ? "Duyệt hồ sơ" : "Từ chối hồ sơ"), [mode]);
  const hint = useMemo(
    () => (mode === "APPROVE" ? "Bạn sắp duyệt hồ sơ này." : "Hãy ghi rõ lý do từ chối để ứng viên biết."),
    [mode]
  );

  if (!open) return null;

  const submit = async () => {
    try {
      setLoading(true);
      await onSubmit({ decision: mode === "APPROVE" ? "APPROVE" : "REJECT", comment });
      toast.success(mode === "APPROVE" ? "Đã duyệt hồ sơ" : "Đã từ chối hồ sơ");
      onClose();
      setComment("");
    } catch (e) {
      toast.error(e?.response?.data?.message || "Có lỗi xảy ra");
    } finally {
      setLoading(false);
    }
  };

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-lg rounded-2xl bg-white shadow-xl">
        <div className="border-b px-5 py-4">
          <div className="text-lg font-semibold">{title}</div>
          <div className="mt-1 text-sm text-slate-600">{hint}</div>
        </div>

        <div className="px-5 py-4">
          <label className="text-sm font-medium text-slate-700">Ghi chú / Lý do</label>
          <textarea
            className="mt-2 w-full resize-none rounded-xl border px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-slate-200"
            rows={5}
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder={mode === "APPROVE" ? "Ghi chú (tuỳ chọn)" : "Lý do từ chối (khuyến nghị)"}
          />
        </div>

        <div className="flex items-center justify-end gap-2 border-t px-5 py-4">
          <button
            className="rounded-xl border px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
            onClick={onClose}
            disabled={loading}
          >
            Huỷ
          </button>
          <button
            className={`rounded-xl px-4 py-2 text-sm font-semibold text-white ${
              mode === "APPROVE" ? "bg-emerald-600 hover:bg-emerald-700" : "bg-rose-600 hover:bg-rose-700"
            } disabled:opacity-60`}
            onClick={submit}
            disabled={loading}
          >
            {loading ? "Đang xử lý..." : mode === "APPROVE" ? "Duyệt" : "Từ chối"}
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
}
```

---

### 4.4 HR Application List page

> File: `src/pages/hr/HrApplicationList.jsx`

```jsx
import React, { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { listApplications } from "@/api/hrApplications";
import ApplicationStatusBadge from "@/components/ApplicationStatusBadge";
import { toast } from "sonner";

const STATUS = ["", "DRAFT", "SUBMITTED", "APPROVED", "REJECTED", "CONTRACT_SENT", "CONTRACT_SIGNED"];

export default function HrApplicationList() {
  const [status, setStatus] = useState("SUBMITTED");
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);

  const totalPages = useMemo(() => data?.totalPages || 0, [data]);

  const fetchData = async () => {
    try {
      setLoading(true);
      const res = await listApplications({ status, keyword, page, size });
      setData(res);
    } catch (e) {
      toast.error(e?.response?.data?.message || "Không tải được danh sách hồ sơ");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [status, page]);

  return (
    <div className="mx-auto w-full max-w-6xl p-4 md:p-6">
      <div className="flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight">Hồ sơ ứng tuyển</h1>
          <p className="mt-1 text-sm text-slate-600">Xem danh sách, lọc và vào chi tiết để duyệt/từ chối.</p>
        </div>

        <div className="flex w-full flex-col gap-2 md:w-auto md:flex-row md:items-center">
          <div className="flex flex-col">
            <span className="text-xs font-medium text-slate-600">Trạng thái</span>
            <select
              className="mt-1 h-10 rounded-xl border bg-white px-3 text-sm outline-none focus:ring-2 focus:ring-slate-200"
              value={status}
              onChange={(e) => {
                setPage(0);
                setStatus(e.target.value);
              }}
            >
              {STATUS.map((s) => (
                <option key={s} value={s}>
                  {s || "ALL"}
                </option>
              ))}
            </select>
          </div>

          <div className="flex flex-col">
            <span className="text-xs font-medium text-slate-600">Từ khoá</span>
            <div className="mt-1 flex gap-2">
              <input
                className="h-10 w-full rounded-xl border px-3 text-sm outline-none focus:ring-2 focus:ring-slate-200 md:w-72"
                placeholder="Ví dụ: Java, Frontend..."
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
              />
              <button
                className="h-10 rounded-xl bg-slate-900 px-4 text-sm font-semibold text-white hover:bg-slate-800"
                onClick={() => {
                  setPage(0);
                  fetchData();
                }}
              >
                Tìm
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="mt-5 overflow-hidden rounded-2xl border bg-white">
        <div className="flex items-center justify-between border-b px-4 py-3">
          <div className="text-sm font-semibold text-slate-800">Danh sách</div>
          <div className="text-xs text-slate-600">{loading ? "Đang tải..." : `Trang ${page + 1}/${Math.max(totalPages, 1)}`}</div>
        </div>

        <div className="w-full overflow-x-auto">
          <table className="min-w-full text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">ID</th>
                <th className="px-4 py-3">Intern</th>
                <th className="px-4 py-3">Vị trí</th>
                <th className="px-4 py-3">Ngày nộp</th>
                <th className="px-4 py-3">Trạng thái</th>
                <th className="px-4 py-3 text-right">Hành động</th>
              </tr>
            </thead>
            <tbody className="divide-y">
              {(data?.content || []).map((row) => (
                <tr key={row.id} className="hover:bg-slate-50">
                  <td className="px-4 py-3 font-medium text-slate-900">{row.id}</td>
                  <td className="px-4 py-3 text-slate-700">{row.internId}</td>
                  <td className="px-4 py-3 text-slate-700">{row.position || "-"}</td>
                  <td className="px-4 py-3 text-slate-700">{row.appliedAt ? new Date(row.appliedAt).toLocaleString() : "-"}</td>
                  <td className="px-4 py-3">
                    <ApplicationStatusBadge status={row.status} />
                  </td>
                  <td className="px-4 py-3 text-right">
                    <Link
                      className="inline-flex items-center rounded-xl border px-3 py-2 text-xs font-semibold text-slate-800 hover:bg-slate-50"
                      to={`/hr/applications/${row.id}`}
                    >
                      Xem chi tiết
                    </Link>
                  </td>
                </tr>
              ))}

              {!loading && (data?.content || []).length === 0 && (
                <tr>
                  <td colSpan={6} className="px-4 py-10 text-center text-sm text-slate-600">
                    Không có dữ liệu
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="flex items-center justify-between border-t px-4 py-3">
          <button
            className="rounded-xl border px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-50"
            onClick={() => setPage((p) => Math.max(p - 1, 0))}
            disabled={page <= 0 || loading}
          >
            Trước
          </button>
          <button
            className="rounded-xl border px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-50"
            onClick={() => setPage((p) => (p + 1 < totalPages ? p + 1 : p))}
            disabled={loading || page + 1 >= totalPages}
          >
            Sau
          </button>
        </div>
      </div>
    </div>
  );
}
```

---

### 4.5 HR Application Detail page

> File: `src/pages/hr/HrApplicationDetail.jsx`

```jsx
import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { getApplication, reviewApplication } from "@/api/hrApplications";
import ApplicationStatusBadge from "@/components/ApplicationStatusBadge";
import ReviewModal from "@/components/ReviewModal";
import { toast } from "sonner";

export default function HrApplicationDetail() {
  const { id } = useParams();
  const nav = useNavigate();

  const [app, setApp] = useState(null);
  const [loading, setLoading] = useState(false);
  const [modal, setModal] = useState(null); // "APPROVE" | "REJECT" | null

  const fetchDetail = async () => {
    try {
      setLoading(true);
      const res = await getApplication(id);
      setApp(res);
    } catch (e) {
      toast.error(e?.response?.data?.message || "Không tải được chi tiết hồ sơ");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDetail();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const doReview = async (payload) => {
    await reviewApplication(id, payload);
    await fetchDetail();
  };

  if (loading && !app) return <div className="p-6 text-sm text-slate-600">Đang tải...</div>;
  if (!app) return <div className="p-6 text-sm text-slate-600">Không có dữ liệu</div>;

  const canReview = app.status === "SUBMITTED";

  return (
    <div className="mx-auto w-full max-w-4xl p-4 md:p-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <button className="text-sm font-semibold text-slate-600 hover:text-slate-900" onClick={() => nav(-1)}>
            ← Quay lại
          </button>
          <h1 className="mt-2 text-2xl font-bold tracking-tight">Chi tiết hồ sơ #{app.id}</h1>
          <div className="mt-2 flex items-center gap-2">
            <ApplicationStatusBadge status={app.status} />
            <span className="text-sm text-slate-600">Intern ID: {app.internId}</span>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button
            className="rounded-xl border px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-50"
            onClick={() => setModal("REJECT")}
            disabled={!canReview}
          >
            Từ chối
          </button>
          <button
            className="rounded-xl bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-700 disabled:opacity-50"
            onClick={() => setModal("APPROVE")}
            disabled={!canReview}
          >
            Duyệt
          </button>
        </div>
      </div>

      <div className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-2">
        <div className="rounded-2xl border bg-white p-4">
          <div className="text-sm font-semibold text-slate-800">Thông tin ứng tuyển</div>
          <div className="mt-3 space-y-2 text-sm text-slate-700">
            <div className="flex justify-between gap-3">
              <span className="text-slate-500">Vị trí</span>
              <span className="font-medium">{app.position || "-"}</span>
            </div>
            <div className="flex justify-between gap-3">
              <span className="text-slate-500">Ngày nộp</span>
              <span className="font-medium">{app.appliedAt ? new Date(app.appliedAt).toLocaleString() : "-"}</span>
            </div>
            <div className="pt-2">
              <div className="text-slate-500">Ghi chú</div>
              <div className="mt-1 whitespace-pre-wrap rounded-xl bg-slate-50 p-3 text-sm">{app.note || "-"}</div>
            </div>
          </div>
        </div>

        <div className="rounded-2xl border bg-white p-4">
          <div className="text-sm font-semibold text-slate-800">Lịch sử xét duyệt</div>
          <div className="mt-3 space-y-2">
            {(app.reviews || []).length === 0 && <div className="text-sm text-slate-600">Chưa có xét duyệt</div>}
            {(app.reviews || []).map((r) => (
              <div key={r.id} className="rounded-xl border p-3 text-sm">
                <div className="flex items-center justify-between">
                  <div className="font-semibold text-slate-900">{r.decision}</div>
                  <div className="text-xs text-slate-500">{r.decidedAt ? new Date(r.decidedAt).toLocaleString() : "-"}</div>
                </div>
                <div className="mt-2 whitespace-pre-wrap text-slate-700">{r.comment || "-"}</div>
                <div className="mt-2 text-xs text-slate-500">Reviewer: {r.reviewerId}</div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <ReviewModal open={modal === "APPROVE"} onClose={() => setModal(null)} onSubmit={doReview} mode="APPROVE" />
      <ReviewModal open={modal === "REJECT"} onClose={() => setModal(null)} onSubmit={doReview} mode="REJECT" />
    </div>
  );
}
```

---

## 5) Router & ProtectedRoute (gợi ý tích hợp)

```jsx
// AppRoutes.jsx (ví dụ)
import React from "react";
import { Routes, Route } from "react-router-dom";
import ProtectedRoute from "@/auth/ProtectedRoute";
import HrApplicationList from "@/pages/hr/HrApplicationList";
import HrApplicationDetail from "@/pages/hr/HrApplicationDetail";

export default function AppRoutes() {
  return (
    <Routes>
      <Route element={<ProtectedRoute allowRoles={["HR"]} />}>
        <Route path="/hr/applications" element={<HrApplicationList />} />
        <Route path="/hr/applications/:id" element={<HrApplicationDetail />} />
      </Route>
    </Routes>
  );
}
```

---

## 6) Checklist test manual (FE)
- [ ] Vào `/hr/applications` thấy danh sách
- [ ] Lọc `SUBMITTED` (mặc định) ra đúng
- [ ] Click “Xem chi tiết” vào `/hr/applications/:id`
- [ ] Với hồ sơ `SUBMITTED`:
  - [ ] bấm Duyệt → status đổi `APPROVED`
  - [ ] bấm Từ chối → status đổi `REJECTED`
- [ ] Với hồ sơ không phải `SUBMITTED`: nút duyệt/từ chối bị disable
- [ ] Refresh vẫn giữ đúng trạng thái

---

## 7) Gợi ý mở rộng ngay sau user story
- Gửi email thông báo kết quả xét duyệt (tách thành user story riêng).
- Cho phép “re-review” với rule:
  - chỉ HR cấp cao hoặc Admin mới override
  - lưu audit trail đầy đủ
- Thêm filter theo intern name/university (join `intern_profiles`).
- Thêm hiển thị tài liệu (CV, đơn xin) từ `intern_documents`.

---

## 8) Notes tích hợp với code sẵn có
- Dự án của bạn đang có sẵn các file security/JWT (ví dụ `SecurityConfig`, `JwtAuthenticationFilter`, `JwtTokenProvider`, `CustomUserDetailsService`).  
  → Bạn chỉ cần đảm bảo:
  1) JWT parse ra principal có **userId** để set `reviewerId` đúng.  
  2) Endpoint `/api/hr/**` được bảo vệ theo role HR.

---

## 9) Chạy dự án (tham khảo)
- Backend: `mvn clean spring-boot:run`
- Frontend: `npm install` → `npm run dev`
