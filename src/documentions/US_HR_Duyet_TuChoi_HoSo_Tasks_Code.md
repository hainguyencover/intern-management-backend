# US: HR duyệt hoặc từ chối hồ sơ (Application Review)
**User story:** Là HR, tôi muốn **duyệt** hoặc **từ chối** hồ sơ để chọn ứng viên phù hợp.  
**Tech stack:** Spring Boot (BE) + React.jsx (FE) + MySQL + TailwindCSS  
**Mục tiêu deliverable:** HR có màn hình xem danh sách hồ sơ, xem chi tiết, thao tác Approve/Reject (có lý do), cập nhật trạng thái, audit log, phân quyền.

---

## 0) Definition of Done (DoD)
- HR xem được **danh sách hồ sơ** (phân trang, filter theo status, keyword).
- HR xem được **chi tiết hồ sơ** (thông tin ứng viên + tài liệu đính kèm).
- HR thao tác **Approve / Reject**:
  - Có modal xác nhận + nhập `comment` (bắt buộc khi Reject).
  - Ghi nhận vào bảng `application_reviews` (reviewer, decision, comment, decided_at).
  - Cập nhật `applications.status` tương ứng.
- RBAC: chỉ role **HR/ADMIN** được duyệt.
- API trả lỗi chuẩn (400/404/403), có validation.
- FE: UX hiện đại (Tailwind), có toast, loading, empty state.
- Có test (backend unit/integration) + checklist manual test FE.

---

## 1) Data model & DB migration (Flyway)
### 1.1 Bảng & quan hệ (MVP)
**applications**
- `id` BIGINT PK
- `intern_id` BIGINT FK → `intern_profiles.id`
- `position` VARCHAR(100)
- `applied_at` DATETIME
- `status` ENUM('DRAFT','SUBMITTED','APPROVED','REJECTED','CONTRACT_SENT','CONTRACT_SIGNED')
- `note` TEXT NULL

**application_reviews**
- `id` BIGINT PK
- `application_id` BIGINT FK → `applications.id`
- `reviewer_id` BIGINT FK → `users.id`
- `decision` ENUM('APPROVED','REJECTED')
- `comment` TEXT NULL
- `decided_at` DATETIME

> Nếu dự án bạn đã có bảng tương tự, hãy chỉ bổ sung cột/constraint thiếu.

### 1.2 Task breakdown (DB)
- [DB-01] Tạo migration `Vx__create_applications.sql`
- [DB-02] Tạo migration `Vy__create_application_reviews.sql`
- [DB-03] Index:
  - `applications(status, applied_at)`
  - `application_reviews(application_id, decided_at)`
- [DB-04] Seed roles HR/ADMIN (nếu thiếu)

### 1.3 SQL mẫu (Flyway)
```sql
-- Vx__create_applications.sql
CREATE TABLE IF NOT EXISTS applications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  intern_id BIGINT NOT NULL,
  position VARCHAR(100) NOT NULL,
  applied_at DATETIME NOT NULL,
  status VARCHAR(30) NOT NULL,
  note TEXT NULL,
  CONSTRAINT fk_app_intern FOREIGN KEY (intern_id) REFERENCES intern_profiles(id)
);

CREATE INDEX idx_app_status_applied ON applications(status, applied_at);

-- Vy__create_application_reviews.sql
CREATE TABLE IF NOT EXISTS application_reviews (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  application_id BIGINT NOT NULL,
  reviewer_id BIGINT NOT NULL,
  decision VARCHAR(20) NOT NULL,
  comment TEXT NULL,
  decided_at DATETIME NOT NULL,
  CONSTRAINT fk_review_app FOREIGN KEY (application_id) REFERENCES applications(id),
  CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id)
);

CREATE INDEX idx_review_app_decided ON application_reviews(application_id, decided_at);
```

---

## 2) Backend (Spring Boot) — Monolith (khuyến nghị MVP)
### 2.1 Packages gợi ý
- `controller.hr` → HR endpoints
- `service` → business logic
- `repository` → JPA repositories
- `domain` → entities + enums
- `dto` → request/response
- `security` → JWT + RBAC
- `exception` → global handler

### 2.2 API thiết kế
#### List applications (HR)
- `GET /api/hr/applications?page=0&size=10&status=SUBMITTED&q=keyword`

#### Get details
- `GET /api/hr/applications/{id}`

#### Approve / Reject
- `POST /api/hr/applications/{id}/decision`
Body:
```json
{ "decision": "APPROVED", "comment": "OK" }
```
Rules:
- Reject => `comment` bắt buộc.
- Chỉ cho phép quyết định khi `applications.status == SUBMITTED` (tránh duyệt lại).

### 2.3 Task breakdown (Backend)
**Core**
- [BE-01] Tạo entity `Application`, `ApplicationReview` + enums.
- [BE-02] Repo: `ApplicationRepository`, `ApplicationReviewRepository`
- [BE-03] DTO:
  - `ApplicationListItemResponse`
  - `ApplicationDetailResponse`
  - `DecisionRequest` (validation)
- [BE-04] Service:
  - `searchApplications(...)`
  - `getDetail(id)`
  - `decide(id, request, reviewerUserId)`
- [BE-05] Controller (HR) + mapping REST.
- [BE-06] Security: `@PreAuthorize("hasRole('HR') or hasRole('ADMIN')")`
- [BE-07] GlobalExceptionHandler: 400/403/404/409 trả JSON chuẩn.
- [BE-08] CORS config cho FE.
- [BE-09] (Tuỳ chọn) Gửi email notification khi duyệt/từ chối.

**Test**
- [BE-T01] Integration test controller: list/detail/decision
- [BE-T02] Unit test service: rule SUBMITTED only + reject comment required

### 2.4 Code mẫu (copy được, tối giản)
> Chỉnh package/import theo project của bạn.

#### Entity (JPA)
```java
// domain/Application.java
@Entity
@Table(name = "applications")
public class Application {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "intern_id", nullable = false)
  private InternProfile intern;

  @Column(nullable = false, length = 100)
  private String position;

  @Column(name="applied_at", nullable = false)
  private LocalDateTime appliedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ApplicationStatus status = ApplicationStatus.SUBMITTED;

  @Lob
  private String note;

  // getters/setters
}

// domain/ApplicationReview.java
@Entity
@Table(name = "application_reviews")
public class ApplicationReview {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="application_id", nullable = false)
  private Application application;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name="reviewer_id", nullable = false)
  private User reviewer;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Decision decision;

  @Lob
  private String comment;

  @Column(name="decided_at", nullable = false)
  private LocalDateTime decidedAt;

  public enum Decision { APPROVED, REJECTED }
}
```

#### Repository
```java
public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {}

public interface ApplicationReviewRepository extends JpaRepository<ApplicationReview, Long> {
  List<ApplicationReview> findByApplicationIdOrderByDecidedAtDesc(Long applicationId);
}
```

#### DTO
```java
// dto/DecisionRequest.java
public class DecisionRequest {
  @NotNull
  private ApplicationReview.Decision decision;

  @Size(max = 2000)
  private String comment;

  // getters/setters
}
```

#### Service (business rules)
```java
@Service
@RequiredArgsConstructor
public class HrApplicationService {
  private final ApplicationRepository applicationRepo;
  private final ApplicationReviewRepository reviewRepo;
  private final UserRepository userRepo;

  @Transactional(readOnly = true)
  public Page<Application> search(String status, String q, Pageable pageable) {
    return applicationRepo.findAll((root, query, cb) -> {
      List<Predicate> ps = new ArrayList<>();
      if (status != null && !status.isBlank()) {
        ps.add(cb.equal(root.get("status"), ApplicationStatus.valueOf(status)));
      }
      if (q != null && !q.isBlank()) {
        String like = "%" + q.trim().toLowerCase() + "%";
        Join<Application, InternProfile> intern = root.join("intern", JoinType.LEFT);
        Join<InternProfile, User> user = intern.join("user", JoinType.LEFT);
        ps.add(cb.or(
          cb.like(cb.lower(user.get("fullName")), like),
          cb.like(cb.lower(user.get("email")), like),
          cb.like(cb.lower(root.get("position")), like)
        ));
      }
      return cb.and(ps.toArray(new Predicate[0]));
    }, pageable);
  }

  @Transactional(readOnly = true)
  public Application getDetail(Long id) {
    return applicationRepo.findById(id).orElseThrow(() -> new NotFoundException("Application not found"));
  }

  @Transactional
  public void decide(Long id, DecisionRequest req, Long reviewerId) {
    Application app = getDetail(id);

    if (app.getStatus() != ApplicationStatus.SUBMITTED) {
      throw new ConflictException("Only SUBMITTED applications can be decided");
    }

    if (req.getDecision() == ApplicationReview.Decision.REJECTED &&
        (req.getComment() == null || req.getComment().isBlank())) {
      throw new BadRequestException("Comment is required when rejecting");
    }

    User reviewer = userRepo.findById(reviewerId)
        .orElseThrow(() -> new NotFoundException("Reviewer not found"));

    ApplicationReview review = new ApplicationReview();
    review.setApplication(app);
    review.setReviewer(reviewer);
    review.setDecision(req.getDecision());
    review.setComment(req.getComment());
    review.setDecidedAt(LocalDateTime.now());
    reviewRepo.save(review);

    app.setStatus(req.getDecision() == ApplicationReview.Decision.APPROVED
        ? ApplicationStatus.APPROVED
        : ApplicationStatus.REJECTED);
    applicationRepo.save(app);
  }
}
```

#### Controller (HR)
```java
@RestController
@RequestMapping("/api/hr/applications")
@RequiredArgsConstructor
public class HrApplicationController {
  private final HrApplicationService service;

  @GetMapping
  @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
  public Page<ApplicationListItemResponse> list(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String q,
      @PageableDefault(size = 10, sort = "appliedAt", direction = Sort.Direction.DESC) Pageable pageable
  ) {
    return service.search(status, q, pageable).map(ApplicationListItemResponse::from);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
  public ApplicationDetailResponse detail(@PathVariable Long id) {
    return ApplicationDetailResponse.from(service.getDetail(id));
  }

  @PostMapping("/{id}/decision")
  @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
  public ResponseEntity<?> decide(@PathVariable Long id, @Valid @RequestBody DecisionRequest req) {
    Long reviewerId = SecurityUtils.currentUserId(); // implement theo JWT của bạn
    service.decide(id, req, reviewerId);
    return ResponseEntity.ok().build();
  }
}
```

---

## 3) Backend — Microservice option (nếu team muốn tách)
> MVP nên monolith. Nếu bắt buộc microservice, đề xuất 2 service:

### 3.1 Services
- **application-service**: applications + reviews + workflow
- **profile-service**: intern_profiles + documents
- (Tuỳ chọn) **notification-service**: email

### 3.2 Task breakdown (Microservice)
- [MS-01] Tách DB schema hoặc dùng DB chung nhưng schema tách module (khuyến nghị schema/module rõ).
- [MS-02] Define contract:
  - application-service gọi profile-service lấy intern summary (REST/Feign)
- [MS-03] Implement gateway (tuỳ chọn) hoặc BFF cho FE.
- [MS-04] Central auth (JWT issuer) + verify token ở từng service.
- [MS-05] Observability: correlation-id logging.

---

## 4) Frontend (React.jsx + Tailwind) — HR Review UI
### 4.1 Routes
- `/hr/applications` → danh sách
- `/hr/applications/:id` → chi tiết + nút Approve/Reject

### 4.2 Task breakdown (Frontend)
**UI**
- [FE-01] Page `HrApplicationList.jsx`: table + filters + pagination
- [FE-02] Page `HrApplicationDetail.jsx`: info card + documents + review history
- [FE-03] Component `DecisionModal.jsx` (Approve/Reject)
- [FE-04] Component `StatusBadge.jsx`
- [FE-05] Loading skeleton + empty state
- [FE-06] Toast thông báo (success/error)

**Data/API**
- [FE-07] API module `hrApplications.api.js` (axios)
- [FE-08] Hook `useHrApplications` (list) + `useHrApplicationDetail`
- [FE-09] Guard route: chỉ HR/ADMIN vào được

### 4.3 Code mẫu FE (tối giản, Tailwind inline)
#### API
```js
// src/api/hrApplications.api.js
import axiosClient from "./axiosClient";

export const hrApplicationsApi = {
  list: (params) => axiosClient.get("/hr/applications", { params }).then(r => r.data),
  detail: (id) => axiosClient.get(`/hr/applications/${id}`).then(r => r.data),
  decide: (id, body) => axiosClient.post(`/hr/applications/${id}/decision`, body),
};
```

#### StatusBadge
```jsx
// src/components/StatusBadge.jsx
export default function StatusBadge({ status }) {
  const map = {
    SUBMITTED: "bg-blue-50 text-blue-700 ring-blue-200",
    APPROVED: "bg-green-50 text-green-700 ring-green-200",
    REJECTED: "bg-red-50 text-red-700 ring-red-200",
    DRAFT: "bg-gray-50 text-gray-700 ring-gray-200",
  };
  const cls = map[status] || "bg-gray-50 text-gray-700 ring-gray-200";
  return (
    <span className={`inline-flex items-center rounded-full px-2 py-1 text-xs font-semibold ring-1 ring-inset ${cls}`}>
      {status}
    </span>
  );
}
```

#### DecisionModal
```jsx
// src/components/DecisionModal.jsx
import { useMemo, useState } from "react";

export default function DecisionModal({ open, onClose, onSubmit, mode }) {
  const [comment, setComment] = useState("");

  const title = useMemo(() => (mode === "APPROVE" ? "Duyệt hồ sơ" : "Từ chối hồ sơ"), [mode]);
  const needComment = mode === "REJECT";

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h3 className="text-lg font-semibold text-slate-900">{title}</h3>
            <p className="mt-1 text-sm text-slate-600">
              {mode === "APPROVE"
                ? "Xác nhận duyệt hồ sơ này."
                : "Vui lòng nhập lý do từ chối (bắt buộc)."}
            </p>
          </div>
          <button onClick={onClose} className="rounded-lg p-2 text-slate-500 hover:bg-slate-100">✕</button>
        </div>

        <div className="mt-4">
          <label className="text-sm font-medium text-slate-700">Ghi chú</label>
          <textarea
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            rows={4}
            className="mt-2 w-full rounded-xl border border-slate-200 bg-white p-3 text-sm text-slate-900 outline-none focus:ring-2 focus:ring-slate-300"
            placeholder={needComment ? "Nhập lý do từ chối..." : "Ghi chú (tuỳ chọn)..."}
          />
          {needComment && <div className="mt-2 text-xs text-slate-500">* Bắt buộc khi Reject</div>}
        </div>

        <div className="mt-6 flex justify-end gap-2">
          <button onClick={onClose} className="rounded-xl px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100">
            Huỷ
          </button>
          <button
            onClick={() => onSubmit(comment)}
            className={`rounded-xl px-4 py-2 text-sm font-semibold text-white ${
              mode === "APPROVE" ? "bg-emerald-600 hover:bg-emerald-700" : "bg-rose-600 hover:bg-rose-700"
            }`}
          >
            Xác nhận
          </button>
        </div>
      </div>
    </div>
  );
}
```

#### HrApplicationList page
```jsx
// src/pages/hr/HrApplicationList.jsx
import { useEffect, useMemo, useState } from "react";
import { hrApplicationsApi } from "@/api/hrApplications.api";
import StatusBadge from "@/components/StatusBadge";
import { Link } from "react-router-dom";

export default function HrApplicationList() {
  const [status, setStatus] = useState("SUBMITTED");
  const [q, setQ] = useState("");
  const [page, setPage] = useState(0);
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(false);

  const params = useMemo(() => ({ status, q, page, size: 10 }), [status, q, page]);

  useEffect(() => {
    let alive = true;
    setLoading(true);
    hrApplicationsApi
      .list(params)
      .then((res) => alive && setData(res))
      .finally(() => alive && setLoading(false));
    return () => { alive = false; };
  }, [params]);

  return (
    <div className="mx-auto w-full max-w-6xl space-y-4 p-4">
      <div className="rounded-2xl bg-white p-4 shadow-sm ring-1 ring-slate-200">
        <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
          <div>
            <h1 className="text-xl font-semibold text-slate-900">Hồ sơ ứng tuyển</h1>
            <p className="mt-1 text-sm text-slate-600">Duyệt hoặc từ chối hồ sơ ứng viên.</p>
          </div>

          <div className="flex flex-col gap-2 sm:flex-row sm:items-center">
            <select
              value={status}
              onChange={(e) => { setStatus(e.target.value); setPage(0); }}
              className="h-10 rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none focus:ring-2 focus:ring-slate-300"
            >
              <option value="SUBMITTED">SUBMITTED</option>
              <option value="APPROVED">APPROVED</option>
              <option value="REJECTED">REJECTED</option>
              <option value="">ALL</option>
            </select>

            <input
              value={q}
              onChange={(e) => { setQ(e.target.value); setPage(0); }}
              className="h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm outline-none focus:ring-2 focus:ring-slate-300 sm:w-72"
              placeholder="Tìm theo tên/email/vị trí..."
            />
          </div>
        </div>
      </div>

      <div className="overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-slate-200">
        <div className="grid grid-cols-12 border-b border-slate-200 bg-slate-50 px-4 py-3 text-xs font-semibold uppercase tracking-wide text-slate-600">
          <div className="col-span-4">Ứng viên</div>
          <div className="col-span-3">Vị trí</div>
          <div className="col-span-2">Ngày nộp</div>
          <div className="col-span-2">Trạng thái</div>
          <div className="col-span-1 text-right">Xem</div>
        </div>

        {loading && <div className="p-4 text-sm text-slate-600">Loading...</div>}

        {!loading && data?.content?.length === 0 && (
          <div className="p-6 text-center text-sm text-slate-600">Không có hồ sơ phù hợp.</div>
        )}

        {!loading && data?.content?.map((it) => (
          <div key={it.id} className="grid grid-cols-12 items-center px-4 py-3 text-sm hover:bg-slate-50">
            <div className="col-span-4">
              <div className="font-semibold text-slate-900">{it.candidateName}</div>
              <div className="text-xs text-slate-600">{it.candidateEmail}</div>
            </div>
            <div className="col-span-3 text-slate-700">{it.position}</div>
            <div className="col-span-2 text-slate-700">{it.appliedAt}</div>
            <div className="col-span-2"><StatusBadge status={it.status} /></div>
            <div className="col-span-1 text-right">
              <Link className="text-slate-900 underline decoration-slate-300 hover:decoration-slate-700" to={`/hr/applications/${it.id}`}>
                Chi tiết
              </Link>
            </div>
          </div>
        ))}

        <div className="flex items-center justify-between border-t border-slate-200 px-4 py-3 text-sm text-slate-600">
          <button
            disabled={page <= 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            className="rounded-xl px-3 py-2 disabled:opacity-50 hover:bg-slate-100"
          >
            ← Prev
          </button>
          <div>
            Page <span className="font-semibold">{data?.number ?? page}</span> / {data?.totalPages ?? 1}
          </div>
          <button
            disabled={(data?.number ?? page) >= ((data?.totalPages ?? 1) - 1)}
            onClick={() => setPage((p) => p + 1)}
            className="rounded-xl px-3 py-2 disabled:opacity-50 hover:bg-slate-100"
          >
            Next →
          </button>
        </div>
      </div>
    </div>
  );
}
```

---

## 5) Manual test checklist (HR review)
- [T-01] HR login → vào `/hr/applications` OK (không 403).
- [T-02] Filter status SUBMITTED/APPROVED/REJECTED hoạt động.
- [T-03] Search theo tên/email/vị trí có kết quả đúng.
- [T-04] Vào detail: hiển thị thông tin intern + documents.
- [T-05] Approve:
  - SUBMITTED → APPROVED, toast success, list cập nhật.
- [T-06] Reject:
  - Nếu comment trống → FE báo lỗi / API 400.
  - Có comment → SUBMITTED → REJECTED.
- [T-07] Không cho duyệt lại:
  - APPROVED/REJECTED → click decision → API trả 409/Conflict.
- [T-08] User không phải HR/ADMIN → 403.

---

## 6) Gợi ý nâng cấp (không bắt buộc MVP)
- Gửi email tự động khi duyệt/từ chối (template + retry).
- Audit log / Activity timeline trên detail.
- Export Excel danh sách hồ sơ.
- Bulk approve/reject (cẩn thận quyền & log).

---

## 7) Notes tích hợp với dự án IMS
- Backend chạy `:8080`, FE dùng `VITE_API_BASE_URL=http://localhost:8080/api` (theo guide setup).
- Nếu bạn đã có module `intern_documents`, ở màn detail hãy render danh sách tài liệu + nút download.
- Nếu đang gặp lỗi 404 download tài liệu (`/api/hr/documents/download/{id}`), hãy đảm bảo FE dùng đúng baseURL `/api` và endpoint đúng controller.
