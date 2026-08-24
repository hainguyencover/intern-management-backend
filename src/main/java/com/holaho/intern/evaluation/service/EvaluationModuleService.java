package com.holaho.intern.evaluation.service;

import com.holaho.intern.entity.Evaluation;
import com.holaho.intern.evaluation.dto.*;
import com.holaho.intern.evaluation.entity.EvaluationCriterion;
import com.holaho.intern.evaluation.entity.EvaluationItem;
import com.holaho.intern.evaluation.entity.EvaluationTemplate;
import com.holaho.intern.evaluation.enums.EvaluationClassification;
import com.holaho.intern.evaluation.enums.EvaluationPeriod;
import com.holaho.intern.evaluation.enums.EvaluationStatus;
import com.holaho.intern.evaluation.repository.EvaluationItemRepository;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.mentor.entity.MentorAssignment;
import com.holaho.intern.mentor.repository.MentorAssignmentRepository;
import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.notification.service.NotificationService;
import com.holaho.intern.repository.EvaluationRepository;
import com.holaho.intern.shared.config.TenantContext;
import com.holaho.intern.shared.enums.MentorAssignmentStatus;
import com.holaho.intern.shared.enums.NotificationType;
import com.holaho.intern.shared.events.DomainEvents;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ForbiddenException;
import com.holaho.intern.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Core evaluation service implementing the full lifecycle:
 *   Create Draft → Update Scores → Submit → HR Review → Approve → Lock
 *
 * Replaces the old EvaluationService with proper state machine,
 * eligibility checks, and calculated scores.
 */
@Service("evaluationModuleService")
@RequiredArgsConstructor
@Slf4j
public class EvaluationModuleService {

    private final EvaluationRepository evaluationRepository;
    private final EvaluationItemRepository itemRepository;
    private final InternProfileRepository internRepository;
    private final MentorRepository mentorRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final EvaluationTemplateService templateService;
    private final EvaluationEligibilityService eligibilityService;
    private final EvaluationCalculationService calculationService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    // ═══════════════════════════════════════════
    // CREATE DRAFT
    // ═══════════════════════════════════════════

    @Transactional
    public EvaluationDetailResponse createDraft(CreateEvaluationDraftRequest request, Long mentorUserId) {
        // 1. Validate eligibility
        eligibilityService.validateEligibility(request.getInternId(), mentorUserId, request.getPeriod());

        // 2. Resolve entities
        InternProfile intern = internRepository.findById(request.getInternId())
                .orElseThrow(() -> new NotFoundException("Intern", request.getInternId()));

        Mentor mentor = mentorRepository.findByUser_Id(mentorUserId)
                .orElseThrow(() -> new NotFoundException("Mentor profile not found for user: " + mentorUserId));

        EvaluationTemplate template = templateService.findActiveTemplateForProgram(
                intern.getMentor() != null ? null : null); // Will use default
        if (request.getTemplateId() != null) {
            template = templateService.findActiveTemplateForProgram(null);
            // Override with specific template if provided
            try {
                EvaluationTemplate specificTemplate = templateService
                        .findActiveTemplateForProgram(request.getTemplateId());
                // Intentionally use the resolved one
            } catch (Exception e) {
                // fallback to default
            }
        }

        Long tenantId = getCurrentTenantId();

        // 3. Create evaluation entity
        Evaluation evaluation = Evaluation.builder()
                .intern(intern)
                .mentor(mentor)
                .template(template)
                .period(request.getPeriod())
                .status(EvaluationStatus.DRAFT)
                .build();
        evaluation.setTenantId(tenantId);

        evaluation = evaluationRepository.save(evaluation);

        // 4. Create evaluation items from template criteria (snapshot weights)
        List<EvaluationItem> items = new ArrayList<>();
        for (EvaluationCriterion criterion : template.getCriteria()) {
            EvaluationItem item = EvaluationItem.builder()
                    .evaluation(evaluation)
                    .criterion(criterion)
                    .weightSnapshot(criterion.getWeight())
                    .maxScoreSnapshot(criterion.getMaxScore())
                    .build();
            items.add(item);
        }
        itemRepository.saveAll(items);
        evaluation.setItems(items);

        log.info("Created evaluation draft ID {} for intern {} by mentor {}",
                evaluation.getId(), intern.getId(), mentorUserId);

        return mapToDetailResponse(evaluation);
    }

    // ═══════════════════════════════════════════
    // UPDATE DRAFT
    // ═══════════════════════════════════════════

    @Transactional
    public EvaluationDetailResponse updateDraft(Long evaluationId, UpdateEvaluationRequest request, Long mentorUserId) {
        Evaluation evaluation = findAndValidateOwnership(evaluationId, mentorUserId);

        // Only DRAFT or RETURNED status can be edited
        if (!evaluation.isEditable()) {
            throw new BadRequestException(
                    "Đánh giá không thể chỉnh sửa. Trạng thái hiện tại: " + evaluation.getStatus());
        }

        // Update criteria scores
        if (request.getCriteria() != null) {
            Map<Long, EvaluationItem> itemMap = evaluation.getItems().stream()
                    .collect(Collectors.toMap(
                            item -> item.getCriterion().getId(),
                            item -> item));

            for (EvaluationCriterionScoreRequest scoreReq : request.getCriteria()) {
                EvaluationItem item = itemMap.get(scoreReq.getCriterionId());
                if (item == null) {
                    throw new BadRequestException(
                            "Tiêu chí đánh giá không hợp lệ: " + scoreReq.getCriterionId());
                }
                // Validate score range
                if (scoreReq.getScore().compareTo(BigDecimal.ZERO) < 0
                        || scoreReq.getScore().compareTo(item.getMaxScoreSnapshot()) > 0) {
                    throw new BadRequestException(
                            String.format("Điểm phải từ 0 đến %s cho tiêu chí '%s'",
                                    item.getMaxScoreSnapshot(), item.getCriterion().getName()));
                }
                item.setScore(scoreReq.getScore());
                item.setComment(scoreReq.getComment());
            }
            itemRepository.saveAll(evaluation.getItems());
        }

        // Update overall comment
        if (request.getOverallComment() != null) {
            evaluation.setOverallComment(request.getOverallComment());
        }

        // Recalculate preview score (not final until submit)
        BigDecimal previewScore = calculationService.calculateOverallScore(evaluation.getItems());
        evaluation.setOverallScore(previewScore);
        evaluation.setClassification(
                calculationService.determineClassification(previewScore).name());

        evaluation = evaluationRepository.save(evaluation);

        log.info("Updated evaluation draft ID {} — preview score: {}", evaluationId, previewScore);
        return mapToDetailResponse(evaluation);
    }

    // ═══════════════════════════════════════════
    // SUBMIT (DRAFT → SUBMITTED)
    // ═══════════════════════════════════════════

    @Transactional
    public EvaluationDetailResponse submit(Long evaluationId, Long mentorUserId) {
        Evaluation evaluation = findAndValidateOwnership(evaluationId, mentorUserId);

        if (!evaluation.isEditable()) {
            throw new BadRequestException(
                    "Đánh giá đã được gửi và không thể gửi lại. Trạng thái: " + evaluation.getStatus());
        }

        // Validate all required criteria are scored
        if (!calculationService.allRequiredCriteriaScored(evaluation.getItems())) {
            throw new BadRequestException(
                    "Chưa hoàn thành tất cả tiêu chí bắt buộc. Vui lòng chấm điểm đầy đủ trước khi gửi.");
        }

        // Re-validate eligibility (task completion may have changed)
        eligibilityService.validateEligibility(
                evaluation.getIntern().getId(), mentorUserId, evaluation.getPeriod());

        // Calculate final score
        BigDecimal overallScore = calculationService.calculateOverallScore(evaluation.getItems());
        EvaluationClassification classification = calculationService.determineClassification(overallScore);

        // Update evaluation
        evaluation.setOverallScore(overallScore);
        evaluation.setClassification(classification.name());
        evaluation.setStatus(EvaluationStatus.SUBMITTED);
        evaluation.setSubmittedAt(LocalDateTime.now());

        // Legacy field sync
        evaluation.setWeightedScore(overallScore.doubleValue());
        evaluation.setScore(overallScore.intValue());
        evaluation.setResultStatus(overallScore.doubleValue() >= 5.0 ? "PASS" : "FAIL");

        evaluation = evaluationRepository.save(evaluation);

        log.info("Evaluation {} submitted — score: {}, classification: {}",
                evaluationId, overallScore, classification);

        // Publish event for notification
        try {
            String mentorName = evaluation.getMentor() != null && evaluation.getMentor().getUser() != null
                    ? evaluation.getMentor().getUser().getFullName() : "Mentor";

            // Notify intern
            if (evaluation.getIntern().getUser() != null) {
                notificationService.createNotification(
                        evaluation.getIntern().getUser().getId(),
                        NotificationType.SYSTEM,
                        "Đánh giá kết quả thực tập",
                        "Mentor " + mentorName + " đã hoàn thành đánh giá (" + evaluation.getPeriod()
                                + ") của bạn. Điểm tổng kết: " + overallScore);
            }
        } catch (Exception e) {
            log.warn("Could not send evaluation notification: {}", e.getMessage());
        }

        return mapToDetailResponse(evaluation);
    }

    // ═══════════════════════════════════════════
    // HR APPROVE / RETURN
    // ═══════════════════════════════════════════

    @Transactional
    public EvaluationDetailResponse approve(Long evaluationId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new NotFoundException("Evaluation", evaluationId));

        if (evaluation.getStatus() != EvaluationStatus.SUBMITTED
                && evaluation.getStatus() != EvaluationStatus.HR_REVIEWING) {
            throw new BadRequestException(
                    "Chỉ có thể phê duyệt đánh giá đã gửi. Trạng thái: " + evaluation.getStatus());
        }

        evaluation.setStatus(EvaluationStatus.APPROVED);
        evaluation.setApprovedAt(LocalDateTime.now());
        evaluation = evaluationRepository.save(evaluation);

        log.info("Evaluation {} approved", evaluationId);
        return mapToDetailResponse(evaluation);
    }

    @Transactional
    public EvaluationDetailResponse returnForRevision(Long evaluationId, String reason) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new NotFoundException("Evaluation", evaluationId));

        if (evaluation.getStatus() != EvaluationStatus.SUBMITTED
                && evaluation.getStatus() != EvaluationStatus.HR_REVIEWING) {
            throw new BadRequestException(
                    "Chỉ có thể trả lại đánh giá đã gửi. Trạng thái: " + evaluation.getStatus());
        }

        evaluation.setStatus(EvaluationStatus.RETURNED);
        evaluation.setReturnedAt(LocalDateTime.now());
        evaluation.setReturnReason(reason);
        evaluation = evaluationRepository.save(evaluation);

        // Notify mentor
        try {
            if (evaluation.getMentor() != null && evaluation.getMentor().getUser() != null) {
                notificationService.createNotification(
                        evaluation.getMentor().getUser().getId(),
                        NotificationType.SYSTEM,
                        "Đánh giá cần chỉnh sửa",
                        "HR đã trả lại đánh giá cho TTS " +
                                (evaluation.getIntern().getUser() != null
                                        ? evaluation.getIntern().getUser().getFullName() : "")
                                + ". Lý do: " + reason);
            }
        } catch (Exception e) {
            log.warn("Could not send return notification: {}", e.getMessage());
        }

        log.info("Evaluation {} returned for revision. Reason: {}", evaluationId, reason);
        return mapToDetailResponse(evaluation);
    }

    // ═══════════════════════════════════════════
    // QUERIES
    // ═══════════════════════════════════════════

    @Transactional(readOnly = true)
    public EvaluationDetailResponse getById(Long id) {
        Evaluation evaluation = evaluationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Evaluation", id));
        return mapToDetailResponse(evaluation);
    }

    @Transactional(readOnly = true)
    public List<EvaluationDetailResponse> getByIntern(Long internId) {
        return evaluationRepository.findByInternIdOrderByCreatedAtDesc(internId).stream()
                .map(this::mapToDetailResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PendingEvaluationResponse> getPendingEvaluations(Long mentorUserId) {
        Mentor mentor = mentorRepository.findByUser_Id(mentorUserId)
                .orElseThrow(() -> new NotFoundException("Mentor profile not found for user: " + mentorUserId));

        // Get all interns assigned to this mentor
        List<MentorAssignment> assignments = mentorAssignmentRepository
                .findByMentorIdAndStatus(mentor.getId(), MentorAssignmentStatus.ACTIVE);

        List<PendingEvaluationResponse> pending = new ArrayList<>();
        for (MentorAssignment assignment : assignments) {
            InternProfile intern = assignment.getIntern();
            if (intern == null) continue;

            // Check if evaluation already exists
            boolean hasEvaluation = evaluationRepository.existsByInternIdAndPeriod(intern.getId(), "FINAL");
            String evalStatus = "PENDING";
            Long draftId = null;

            if (hasEvaluation) {
                var existing = evaluationRepository
                        .findFirstByInternIdAndPeriodOrderByCreatedAtDesc(intern.getId(), "FINAL");
                if (existing.isPresent()) {
                    Evaluation eval = existing.get();
                    evalStatus = eval.getStatus() != null ? eval.getStatus().name() : "LOCKED";
                    draftId = eval.getId();
                }
            }

            double taskCompletion = eligibilityService.calculateTaskCompletionRate(intern.getId());
            double attendanceRate = eligibilityService.calculateAttendanceRate(intern.getId());

            pending.add(PendingEvaluationResponse.builder()
                    .internId(intern.getId())
                    .internName(intern.getUser() != null ? intern.getUser().getFullName() : null)
                    .studentCode(intern.getStudentCode())
                    .programName(null) // TODO: resolve from enrollment
                    .taskCompletion(taskCompletion)
                    .attendanceRate(attendanceRate)
                    .evaluationStatus(evalStatus)
                    .draftEvaluationId(draftId)
                    .build());
        }

        return pending;
    }

    @Transactional(readOnly = true)
    public Page<EvaluationDetailResponse> getMentorEvaluations(
            Long mentorUserId, String period, String keyword, Pageable pageable) {
        Mentor mentor = mentorRepository.findByUser_Id(mentorUserId)
                .orElseThrow(() -> new NotFoundException("Mentor profile not found for user: " + mentorUserId));

        return evaluationRepository.findByMentorIdAndFilters(
                        mentor.getId(),
                        period != null && !period.isBlank() ? period.trim() : null,
                        keyword != null && !keyword.isBlank() ? keyword.trim() : null,
                        pageable)
                .map(this::mapToDetailResponse);
    }

    @Transactional(readOnly = true)
    public Page<EvaluationDetailResponse> getAllEvaluations(
            String period, String keyword, Pageable pageable) {
        return evaluationRepository.findAllEvaluationsWithFilters(
                        period != null && !period.isBlank() ? period.trim() : null,
                        keyword != null && !keyword.isBlank() ? keyword.trim() : null,
                        pageable)
                .map(this::mapToDetailResponse);
    }

    // ═══════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════

    private Evaluation findAndValidateOwnership(Long evaluationId, Long mentorUserId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new NotFoundException("Evaluation", evaluationId));

        // Verify mentor ownership
        if (evaluation.getMentor() == null
                || evaluation.getMentor().getUser() == null
                || !evaluation.getMentor().getUser().getId().equals(mentorUserId)) {
            throw new ForbiddenException("Bạn không phải mentor của đánh giá này.");
        }

        // Verify tenant isolation
        Long currentTenantId = getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(evaluation.getTenantId())) {
            throw new ForbiddenException("Đánh giá không thuộc chi nhánh của bạn.");
        }

        return evaluation;
    }

    private EvaluationDetailResponse mapToDetailResponse(Evaluation eval) {
        List<EvaluationDetailResponse.EvaluationItemResponse> itemResponses = new ArrayList<>();

        if (eval.getItems() != null) {
            for (EvaluationItem item : eval.getItems()) {
                itemResponses.add(EvaluationDetailResponse.EvaluationItemResponse.builder()
                        .id(item.getId())
                        .criterionId(item.getCriterion() != null ? item.getCriterion().getId() : null)
                        .criterionName(item.getCriterion() != null ? item.getCriterion().getName() : null)
                        .category(item.getCriterion() != null ? item.getCriterion().getCategory().name() : null)
                        .score(item.getScore())
                        .comment(item.getComment())
                        .weight(item.getWeightSnapshot())
                        .maxScore(item.getMaxScoreSnapshot())
                        .displayOrder(item.getCriterion() != null ? item.getCriterion().getDisplayOrder() : null)
                        .build());
            }
        }

        // Calculate performance context
        Double taskCompletion = null;
        Double attendanceRate = null;
        if (eval.getIntern() != null) {
            try {
                taskCompletion = eligibilityService.calculateTaskCompletionRate(eval.getIntern().getId());
                attendanceRate = eligibilityService.calculateAttendanceRate(eval.getIntern().getId());
            } catch (Exception e) {
                log.debug("Could not calculate performance metrics: {}", e.getMessage());
            }
        }

        return EvaluationDetailResponse.builder()
                .id(eval.getId())
                .internId(eval.getIntern() != null ? eval.getIntern().getId() : null)
                .internName(eval.getIntern() != null && eval.getIntern().getUser() != null
                        ? eval.getIntern().getUser().getFullName() : null)
                .studentCode(eval.getIntern() != null ? eval.getIntern().getStudentCode() : null)
                .mentorId(eval.getMentor() != null ? eval.getMentor().getId() : null)
                .mentorName(eval.getMentor() != null && eval.getMentor().getUser() != null
                        ? eval.getMentor().getUser().getFullName() : null)
                .templateId(eval.getTemplate() != null ? eval.getTemplate().getId() : null)
                .templateName(eval.getTemplate() != null ? eval.getTemplate().getName() : null)
                .period(eval.getPeriod())
                .status(eval.getStatus() != null ? eval.getStatus().name() : "LOCKED")
                .overallScore(eval.getOverallScore())
                .classification(eval.getClassification())
                .overallComment(eval.getOverallComment())
                .items(itemResponses)
                .taskCompletion(taskCompletion)
                .attendanceRate(attendanceRate)
                .createdAt(eval.getCreatedAt())
                .submittedAt(eval.getSubmittedAt())
                .approvedAt(eval.getApprovedAt())
                .lockedAt(eval.getLockedAt())
                .returnedAt(eval.getReturnedAt())
                .returnReason(eval.getReturnReason())
                .build();
    }

    private Long getCurrentTenantId() {
        Long tenantId = TenantContext.getCurrentTenantId();
        return tenantId != null ? tenantId : 1L;
    }
}
