package com.holaho.intern.service;

import com.holaho.intern.entity.Program;
import com.holaho.intern.repository.ApplicationStatusHistoryRepository;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.shared.exception.NotFoundException;

import com.holaho.intern.shared.dto.request.ApplicationSubmitRequest;
import com.holaho.intern.shared.dto.request.CreateApplicationRequest;
import com.holaho.intern.shared.dto.request.ReviewApplicationRequest;
import com.holaho.intern.shared.dto.response.ApplicationResponse;
import com.holaho.intern.entity.Application;
import com.holaho.intern.entity.ApplicationReview;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.shared.enums.ApplicationStatus;
import com.holaho.intern.shared.enums.ReviewDecision;
import com.holaho.intern.repository.ApplicationRepository;
import com.holaho.intern.repository.ApplicationReviewRepository;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.intern.repository.InternDocumentRepository;
import com.holaho.intern.repository.ProgramRepository;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationReviewRepository reviewRepository;
    private final InternProfileRepository internProfileRepository;
    private final InternDocumentRepository internDocumentRepository;
    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final AiService aiService;
    private final StorageService storageService;
    private final ApplicationEventPublisher eventPublisher;
    private final ApplicationStatusHistoryRepository statusHistoryRepository;
    private final com.holaho.intern.shared.workflow.ApplicationStateMachine stateMachine;

    @Transactional
    public ApplicationResponse submit(ApplicationSubmitRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        // 1. Email Verification Requirement Check
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new com.holaho.intern.shared.exception.ForbiddenException(
                    "Vui lòng xác thực email trước khi nộp hồ sơ");
        }

        InternProfile intern = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Intern profile not found for user: " + userId));

        Long internId = intern.getId();

        // 2. BR-001 Check: No existing SUBMITTED application allowed
        boolean hasPending = applicationRepository.existsByIntern_IdAndStatus(
                internId, ApplicationStatus.SUBMITTED);

        if (hasPending) {
            throw new ConflictException("Bạn đã có đơn ứng tuyển đang chờ xét duyệt");
        }

        // 3. BR-002 Check: Program must be ACTIVE
        com.holaho.intern.entity.Program program = programRepository.findById(request.getProgramId())
                .orElseThrow(() -> new NotFoundException("Program not found: " + request.getProgramId()));

        if (program.getStatus() != com.holaho.intern.shared.enums.ProgramStatus.ACTIVE) {
            throw new ConflictException("Chương trình thực tập hiện tại không mở nhận hồ sơ");
        }

        Application application = new Application();
        application.setIntern(intern);
        application.setProgram(program);
        application.setPosition(request.getPosition());
        application.setNote(request.getNote());
        application.setAppliedAt(LocalDateTime.now());
        application.setStatus(ApplicationStatus.SUBMITTED);

        application = applicationRepository.save(application);

        log.info("Application submitted by intern: {} for program: {}", internId, request.getProgramId());

        // Publish event for email notification dispatch
        eventPublisher.publishEvent(new com.holaho.intern.shared.events.ApplicationSubmittedEvent(
                this,
                application.getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                program.getName()
        ));

        return toResponse(application);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getByInternId(Long internId) {
        List<Application> applications = applicationRepository.findByIntern_Id(internId);
        return applications.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ApplicationResponse createApplication(CreateApplicationRequest request, Long internId) {
        InternProfile intern = internProfileRepository.findById(internId)
                .orElseThrow(() -> new NotFoundException("Intern profile", internId));

        // Check if already has pending/approved application
        if (applicationRepository.existsByIntern_IdAndStatus(internId, ApplicationStatus.SUBMITTED) ||
                applicationRepository.existsByIntern_IdAndStatus(internId, ApplicationStatus.APPROVED)) {
            throw new ConflictException("Bạn đã có đơn ứng tuyển đang chờ hoặc đã được duyệt");
        }

        Application application = new Application();
        application.setIntern(intern);
        application.setPosition(request.getPosition());
        application.setAppliedAt(LocalDateTime.now());
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setNote(request.getNote());

        application = applicationRepository.save(application);
        log.info("Created application for intern: {}", internId);

        return mapToResponse(application);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getById(Long id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Application", id));
        return toResponse(application);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getMyApplications(Long userId) {
        InternProfile intern = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Intern profile not found"));

        List<Application> applications = applicationRepository.findByIntern_Id(intern.getId());
        return applications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> searchApplications(ApplicationStatus status, String keyword, Pageable pageable) {
        return applicationRepository.searchApplications(status, keyword, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplication(Long id) {
        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Application not found"));
        return mapToResponse(application);
    }

    @Transactional
    public ApplicationResponse reviewApplication(Long applicationId, ReviewApplicationRequest request,
            Long reviewerId) {
        Application application = applicationRepository.findByIdWithIntern(applicationId)
                .orElseThrow(() -> new NotFoundException("Application", applicationId));

        ApplicationStatus targetStatus = request.decision() == ReviewDecision.APPROVE ? ApplicationStatus.APPROVED : ApplicationStatus.REJECTED;
        stateMachine.validateTransition(application.getStatus(), targetStatus);

        // Check US-007-AC-02: Mandatory comment for REJECT
        if (request.decision() == ReviewDecision.REJECT && (request.comment() == null || request.comment().isBlank())) {
            throw new BadRequestException("Vui lòng nhập lý do từ chối hồ sơ (US-007-AC-02)");
        }

        // Check US-007-AC-03: Required documents check for APPROVE
        if (request.decision() == ReviewDecision.APPROVE) {
            InternProfile profile = application.getIntern();
            boolean hasCvInProfile = profile != null && profile.getCvUrl() != null && !profile.getCvUrl().isBlank();
            boolean hasCvInDocs = profile != null && (
                    internDocumentRepository.existsByIntern_IdAndType(profile.getId(), "CV") ||
                    internDocumentRepository.existsByInternIdAndType(profile.getId(), "CV")
            );
            if (!hasCvInProfile && !hasCvInDocs) {
                throw new ConflictException("Không thể phê duyệt hồ sơ chưa có tài liệu CV bắt buộc (US-007-AC-03)");
            }
        }

        // Check if already reviewed
        if (reviewRepository.existsByApplicationId(applicationId)) {
            throw new ConflictException("Đơn ứng tuyển đã được duyệt trước đó");
        }

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new NotFoundException("Reviewer", reviewerId));

        // Create review
        ApplicationReview review = new ApplicationReview();
        review.setApplication(application);
        review.setReviewer(reviewer);
        review.setDecision(request.decision());
        review.setComment(request.comment());
        review.setDecidedAt(LocalDateTime.now());

        ApplicationStatus oldStatus = application.getStatus();
        reviewRepository.save(review);

        // Update application status
        if (request.decision() == ReviewDecision.APPROVE) {
            application.setStatus(ApplicationStatus.APPROVED);

            // Auto-transition candidate profile status to ONBOARDING
            InternProfile profile = application.getIntern();
            profile.setStatus("ONBOARDING");
            internProfileRepository.save(profile);

            // Assign ROLE_INTERN to user & mark email verified upon HR Approval
            User user = profile.getUser();
            user.setEmailVerified(true);
            com.holaho.intern.user.entity.Role internRole = roleRepository.findByCode("INTERN")
                    .orElseThrow(() -> new NotFoundException("Role INTERN không tồn tại"));
            user.getRoles().add(internRole);
            userRepository.save(user);

            // Auto-approve pending intern documents (CV, etc.)
            java.util.List<com.holaho.intern.intern.entity.InternDocument> pendingDocs = internDocumentRepository.findByInternId(profile.getId());
            for (com.holaho.intern.intern.entity.InternDocument doc : pendingDocs) {
                if ("PENDING".equalsIgnoreCase(doc.getStatus())) {
                    doc.setStatus("APPROVED");
                    doc.setReviewedBy(reviewer);
                    doc.setReviewedAt(LocalDateTime.now());
                    doc.setReviewNote("Tự động phê duyệt khi HR chấp nhận Đơn ứng tuyển");
                    internDocumentRepository.save(doc);
                }
            }

            log.info("Auto activated InternProfile to ONBOARDING, verified email, approved documents and assigned ROLE_INTERN for user: {}", user.getEmail());

            // Publish Domain Event for notification/email
            eventPublisher.publishEvent(new com.holaho.intern.shared.events.DomainEvents.ApplicationAcceptedEvent(
                    this,
                    application.getId(),
                    user.getId(),
                    user.getFullName(),
                    user.getEmail()
            ));
        } else {
            application.setStatus(ApplicationStatus.REJECTED);

            User user = application.getIntern().getUser();
            eventPublisher.publishEvent(new com.holaho.intern.shared.events.DomainEvents.ApplicationRejectedEvent(
                    this,
                    application.getId(),
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    request.comment()
            ));
        }

        application = applicationRepository.save(application);
        recordStatusChange(application, oldStatus, application.getStatus(), reviewer, request.comment());
        log.info("Reviewed application {} with decision: {}", applicationId, request.decision());

        return mapToResponse(application);
    }

    // -------------------------------------------------------------
    // Enterprise Recruitment Workflow Methods (US-047 -> US-053)
    // -------------------------------------------------------------

    private void recordStatusChange(Application app, ApplicationStatus fromStatus, ApplicationStatus toStatus, User actor, String reason) {
        try {
            com.holaho.intern.entity.ApplicationStatusHistory hist =
                    new com.holaho.intern.entity.ApplicationStatusHistory(app, fromStatus, toStatus, actor, reason);
            statusHistoryRepository.save(hist);
        } catch (Exception e) {
            log.warn("Could not save status history trace: {}", e.getMessage());
        }
    }

    /**
     * US-048: HR Start Review & Assign Reviewer
     * POST /api/v1/applications/{id}/start-review
     */
    @Transactional
    public ApplicationResponse startReview(Long applicationId, Long reviewerId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application", applicationId));

        ApplicationStatus oldStatus = app.getStatus();
        stateMachine.validateTransition(oldStatus, ApplicationStatus.REVIEWING);

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new NotFoundException("Reviewer", reviewerId));

        app.setStatus(ApplicationStatus.REVIEWING);
        app = applicationRepository.save(app);

        recordStatusChange(app, oldStatus, ApplicationStatus.REVIEWING, reviewer, "HR bắt đầu quy trình rà soát");
        log.info("HR {} started reviewing application {}", reviewerId, applicationId);

        return mapToResponse(app);
    }

    /**
     * US-049: Automated Eligibility Screening Engine
     * GET /api/v1/applications/{id}/eligibility
     */
    @Transactional(readOnly = true)
    public com.holaho.intern.shared.dto.response.EligibilityCheckResponse checkEligibility(Long applicationId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application", applicationId));

        InternProfile profile = app.getIntern();
        User user = profile.getUser();

        java.util.List<com.holaho.intern.shared.dto.response.EligibilityCheckResponse.RuleCheck> checks = new java.util.ArrayList<>();

        // 1. Email Verification Rule
        boolean emailPassed = Boolean.TRUE.equals(user.getEmailVerified());
        checks.add(com.holaho.intern.shared.dto.response.EligibilityCheckResponse.RuleCheck.builder()
                .rule("EMAIL_VERIFIED")
                .passed(emailPassed)
                .message(emailPassed ? "Email đã được xác thực" : "Email chưa được xác thực (Yêu cầu xác thực trước khi trúng tuyển)")
                .build());

        // 2. Required CV Rule
        boolean cvPassed = (profile.getCvUrl() != null && !profile.getCvUrl().isBlank()) ||
                internDocumentRepository.existsByIntern_IdAndType(profile.getId(), "CV");
        checks.add(com.holaho.intern.shared.dto.response.EligibilityCheckResponse.RuleCheck.builder()
                .rule("REQUIRED_CV_DOCUMENT")
                .passed(cvPassed)
                .message(cvPassed ? "Tài liệu CV hợp lệ" : "Thiếu tài liệu CV bắt buộc (US-007-AC-03)")
                .build());

        // 3. Minimum GPA Rule (Threshold 2.5 / 4.0)
        Double gpa = profile.getGpa();
        boolean gpaPassed = (gpa != null && gpa >= 2.5);
        checks.add(com.holaho.intern.shared.dto.response.EligibilityCheckResponse.RuleCheck.builder()
                .rule("MINIMUM_GPA")
                .passed(gpaPassed)
                .message(gpaPassed ? String.format("GPA đạt yêu cầu: %.2f / 4.0", gpa) : (gpa == null ? "Chưa cập nhật GPA" : String.format("GPA %.2f chưa đạt mức tối thiểu 2.5", gpa)))
                .build());

        // 4. University & Major Completeness
        boolean eduPassed = profile.getUniversity() != null && !profile.getUniversity().isBlank() &&
                profile.getMajor() != null && !profile.getMajor().isBlank();
        checks.add(com.holaho.intern.shared.dto.response.EligibilityCheckResponse.RuleCheck.builder()
                .rule("EDUCATION_INFO")
                .passed(eduPassed)
                .message(eduPassed ? "Thông tin Trường & Chuyên ngành đầy đủ" : "Thiếu thông tin Trường đại học hoặc Chuyên ngành")
                .build());

        // 5. Active Application Duplicate Check
        boolean isDuplicate = applicationRepository.existsByIntern_IdAndStatus(profile.getId(), ApplicationStatus.SUBMITTED) &&
                !app.getStatus().equals(ApplicationStatus.SUBMITTED);
        checks.add(com.holaho.intern.shared.dto.response.EligibilityCheckResponse.RuleCheck.builder()
                .rule("DUPLICATE_APPLICATION")
                .passed(!isDuplicate)
                .message(!isDuplicate ? "Không có đơn ứng tuyển trùng lặp" : "Cảnh báo: Ứng viên có đơn ứng tuyển khác đang chờ xử lý")
                .build());

        boolean overallEligible = checks.stream().allMatch(com.holaho.intern.shared.dto.response.EligibilityCheckResponse.RuleCheck::isPassed);

        return com.holaho.intern.shared.dto.response.EligibilityCheckResponse.builder()
                .eligible(overallEligible)
                .checks(checks)
                .build();
    }

    /**
     * US-050: Request Revision by HR
     * POST /api/v1/applications/{id}/request-revision
     */
    @Transactional
    public ApplicationResponse requestRevision(Long applicationId, Long reviewerId, String comment) {
        if (comment == null || comment.isBlank()) {
            throw new BadRequestException("Lý do yêu cầu bổ sung hồ sơ không được để trống");
        }

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application", applicationId));

        ApplicationStatus oldStatus = app.getStatus();
        stateMachine.validateTransition(oldStatus, ApplicationStatus.NEEDS_REVISION);

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new NotFoundException("Reviewer", reviewerId));

        app.setStatus(ApplicationStatus.NEEDS_REVISION);
        app.setNote("Yêu cầu bổ sung: " + comment);
        app = applicationRepository.save(app);

        recordStatusChange(app, oldStatus, ApplicationStatus.NEEDS_REVISION, reviewer, comment);

        // Notify Candidate
        User candidate = app.getIntern().getUser();
        eventPublisher.publishEvent(new com.holaho.intern.shared.events.DomainEvents.ApplicationRejectedEvent(
                this,
                app.getId(),
                candidate.getId(),
                candidate.getFullName(),
                candidate.getEmail(),
                "Yêu cầu bổ sung hồ sơ: " + comment
        ));

        log.info("HR {} requested revision for application {}", reviewerId, applicationId);
        return mapToResponse(app);
    }

    /**
     * US-050: Candidate Resubmit Application
     * POST /api/v1/applications/{id}/resubmit
     */
    @Transactional
    public ApplicationResponse resubmit(Long applicationId, Long userId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application", applicationId));

        if (!app.getIntern().getUser().getId().equals(userId)) {
            throw new com.holaho.intern.shared.exception.ForbiddenException("Bạn không có quyền resubmit đơn ứng tuyển này");
        }

        ApplicationStatus oldStatus = app.getStatus();
        stateMachine.validateTransition(oldStatus, ApplicationStatus.SUBMITTED);

        app.setStatus(ApplicationStatus.SUBMITTED);
        app.setAppliedAt(LocalDateTime.now());
        app = applicationRepository.save(app);

        recordStatusChange(app, oldStatus, ApplicationStatus.SUBMITTED, app.getIntern().getUser(), "Candidate resubmitted profile after revision");
        log.info("Candidate {} resubmitted application {}", userId, applicationId);

        return mapToResponse(app);
    }

    /**
     * US-051: Status Audit History
     * GET /api/v1/applications/{id}/history
     */
    @Transactional(readOnly = true)
    public java.util.List<com.holaho.intern.shared.dto.response.StatusHistoryResponse> getStatusHistory(Long applicationId) {
        java.util.List<com.holaho.intern.entity.ApplicationStatusHistory> history = statusHistoryRepository.findByApplicationIdWithUser(applicationId);
        return history.stream().map(h -> com.holaho.intern.shared.dto.response.StatusHistoryResponse.builder()
                .id(h.getId())
                .fromStatus(h.getFromStatus() != null ? h.getFromStatus().name() : null)
                .toStatus(h.getToStatus().name())
                .changedById(h.getChangedBy() != null ? h.getChangedBy().getId() : null)
                .changedByName(h.getChangedBy() != null ? h.getChangedBy().getFullName() : "SYSTEM")
                .reason(h.getReason())
                .createdAt(h.getCreatedAt())
                .build()).toList();
    }

    /**
     * US-052: Review Queue & Overdue SLA Statistics
     * GET /api/v1/applications/queue
     */
    @Transactional(readOnly = true)
    public com.holaho.intern.shared.dto.response.ReviewQueueStatsDto getReviewQueueStats() {
        long pendingCount = applicationRepository.countByStatus(ApplicationStatus.SUBMITTED);
        long inReviewCount = applicationRepository.countByStatus(ApplicationStatus.REVIEWING);
        long needsRevisionCount = applicationRepository.countByStatus(ApplicationStatus.NEEDS_REVISION);

        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        long overdueCount = applicationRepository.findAll().stream()
                .filter(a -> a.getStatus() == ApplicationStatus.SUBMITTED && a.getAppliedAt() != null && a.getAppliedAt().isBefore(threeDaysAgo))
                .count();

        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        long approvedToday = applicationRepository.findAll().stream()
                .filter(a -> a.getStatus() == ApplicationStatus.APPROVED && a.getUpdatedAt() != null && a.getUpdatedAt().isAfter(startOfDay))
                .count();
        long rejectedToday = applicationRepository.findAll().stream()
                .filter(a -> a.getStatus() == ApplicationStatus.REJECTED && a.getUpdatedAt() != null && a.getUpdatedAt().isAfter(startOfDay))
                .count();

        return com.holaho.intern.shared.dto.response.ReviewQueueStatsDto.builder()
                .pendingReviewCount(pendingCount)
                .inReviewCount(inReviewCount)
                .overdueCount(overdueCount)
                .needsRevisionCount(needsRevisionCount)
                .approvedTodayCount(approvedToday)
                .rejectedTodayCount(rejectedToday)
                .build();
    }

    /**
     * US-053: Candidate Result View
     * GET /api/v1/applications/me/latest-result
     */
    @Transactional(readOnly = true)
    public com.holaho.intern.shared.dto.response.CandidateResultResponse getLatestCandidateResult(Long userId) {
        InternProfile profile = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Profile not found for user: " + userId));

        java.util.List<Application> apps = applicationRepository.findByIntern_Id(profile.getId());
        if (apps.isEmpty()) {
            throw new NotFoundException("Chưa có đơn ứng tuyển nào");
        }

        Application latestApp = apps.stream()
                .max(java.util.Comparator.comparing(Application::getAppliedAt))
                .orElse(apps.get(0));

        var latestReview = reviewRepository.findFirstByApplicationIdOrderByDecidedAtDesc(latestApp.getId());

        String nextStep;
        switch (latestApp.getStatus()) {
            case APPROVED -> nextStep = "Chúc mừng bạn đã trúng tuyển! Hãy truy cập Hồ sơ cá nhân để chờ nhận Hợp đồng thực tập.";
            case REJECTED -> nextStep = "Rất tiếc hồ sơ chưa phù hợp đợt này. Bạn có thể cập nhật thông tin và nộp đơn mới cho các chương trình tiếp theo.";
            case NEEDS_REVISION -> nextStep = "Hồ sơ của bạn cần bổ sung thông tin. Vui lòng cập nhật CV/Hồ sơ và bấm Nộp lại (Resubmit).";
            case REVIEWING -> nextStep = "Bộ phận HR đang rà soát chi tiết hồ sơ của bạn. Kết quả sẽ được cập nhật sớm.";
            default -> nextStep = "Đơn ứng tuyển đã được ghi nhận và đang chờ HR bắt đầu xét duyệt.";
        }

        return com.holaho.intern.shared.dto.response.CandidateResultResponse.builder()
                .applicationId(latestApp.getId())
                .status(latestApp.getStatus().name())
                .programName(latestApp.getProgram() != null ? latestApp.getProgram().getName() : "N/A")
                .position(latestApp.getPosition())
                .reviewerName(latestReview.map(r -> r.getReviewer().getFullName()).orElse("Hội đồng Tuyển dụng HR"))
                .reviewComment(latestReview.map(ApplicationReview::getComment).orElse(latestApp.getNote()))
                .decidedAt(latestReview.map(ApplicationReview::getDecidedAt).orElse(latestApp.getUpdatedAt()))
                .nextStep(nextStep)
                .build();
    }

    @Transactional
    public void triggerAiScreening(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("Application not found"));

        String cvUrl = application.getIntern().getCvUrl();
        if (cvUrl == null || cvUrl.isEmpty()) {
            log.warn("Cannot trigger AI screening for application {}: No CV URL found", applicationId);
            return;
        }

        try {
            log.info("Triggering AI screening for application {} with CV: {}", applicationId, cvUrl);
            org.springframework.core.io.Resource resource = storageService.loadFileAsResource(cvUrl);
            byte[] fileContent = resource.getContentAsByteArray();

            var screeningResult = aiService.screenCv(fileContent, cvUrl);

            application.setAiScore(screeningResult.getScore());
            application.setAiSkills(String.join(", ", screeningResult.getSkills()));
            application.setAiSummary(screeningResult.getSummary());
            application.setAiRecommendation(screeningResult.getRecommendation());

            applicationRepository.save(application);
            log.info("AI screening completed for application {}", applicationId);
        } catch (Exception e) {
            log.error("Failed to perform AI screening for application {}", applicationId, e);
        }
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(Long id) {
        Application application = applicationRepository.findByIdWithIntern(id)
                .orElseThrow(() -> new NotFoundException("Application", id));
        return mapToResponse(application);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getApplicationsByStatus(ApplicationStatus status, Pageable pageable) {
        return applicationRepository.findByStatus(status, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByInternId(Long internId) {
        return applicationRepository.findByInternId(internId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse.ApplicationReviewResponse> getReviewsByApplicationId(Long applicationId) {
        return reviewRepository.findByApplicationIdOrderByDecidedAtDesc(applicationId).stream()
                .map(this::mapReviewToResponse)
                .collect(Collectors.toList());
    }

    private ApplicationResponse mapToResponse(Application application) {
        List<ApplicationResponse.ApplicationReviewResponse> reviews = reviewRepository
                .findByApplicationIdOrderByDecidedAtDesc(application.getId()).stream()
                .map(this::mapReviewToResponse)
                .collect(Collectors.toList());

        return ApplicationResponse.builder()
                .id(application.getId())
                .internId(application.getIntern().getId())
                .internName(application.getIntern().getUser().getFullName())
                .internEmail(application.getIntern().getUser().getEmail())
                .programId(application.getProgram() != null ? application.getProgram().getId() : null)
                .programName(application.getProgram() != null ? application.getProgram().getName() : null)
                .position(application.getPosition())
                .appliedAt(application.getAppliedAt())
                .status(String.valueOf(application.getStatus()))
                .note(application.getNote())
                .reviews(reviews)
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .aiScore(application.getAiScore())
                .aiSkills(
                        application.getAiSkills() != null ? List.of(application.getAiSkills().split(", ")) : List.of())
                .aiSummary(application.getAiSummary())
                .aiRecommendation(application.getAiRecommendation())
                .build();
    }

    private ApplicationResponse toResponse(Application application) {
        ApplicationResponse response = new ApplicationResponse();
        response.setId(application.getId());
        response.setInternId(application.getIntern().getId());
        response.setInternName(application.getIntern().getUser().getFullName());
        response.setInternEmail(application.getIntern().getUser().getEmail());
        if (application.getProgram() != null) {
            response.setProgramId(application.getProgram().getId());
            response.setProgramName(application.getProgram().getName());
        }
        response.setPosition(application.getPosition());
        response.setAppliedAt(application.getAppliedAt());
        response.setStatus(application.getStatus().name());
        response.setNote(application.getNote());
        response.setCreatedAt(application.getCreatedAt());
        return response;
    }

    private ApplicationResponse.ApplicationReviewResponse mapReviewToResponse(ApplicationReview review) {
        return ApplicationResponse.ApplicationReviewResponse.builder()
                .id(review.getId())
                .applicationId(review.getApplication().getId())
                .reviewerId(review.getReviewer().getId())
                .reviewerName(review.getReviewer().getFullName())
                .decision(review.getDecision())
                .comment(review.getComment())
                .decidedAt(review.getDecidedAt())
                .build();
    }
}
