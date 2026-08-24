package com.holaho.intern.report.service;

import com.holaho.intern.entity.Program;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.notification.service.NotificationService;
import com.holaho.intern.report.dto.*;
import com.holaho.intern.report.entity.WeeklyReport;
import com.holaho.intern.report.entity.WeeklyReportFeedback;
import com.holaho.intern.report.enums.WeeklyReportStatus;
import com.holaho.intern.report.repository.WeeklyReportFeedbackRepository;
import com.holaho.intern.report.repository.WeeklyReportRepository;
import com.holaho.intern.shared.config.TenantContext;
import com.holaho.intern.shared.enums.NotificationType;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.shared.exception.ForbiddenException;
import com.holaho.intern.shared.exception.NotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service("newWeeklyReportServiceImpl")
@RequiredArgsConstructor
@Slf4j
public class WeeklyReportServiceImpl implements WeeklyReportService {

    private final WeeklyReportRepository weeklyReportRepository;
    private final WeeklyReportFeedbackRepository feedbackRepository;
    private final InternProfileRepository internProfileRepository;
    private final MentorRepository mentorRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public WeeklyReportResponse createDraft(Long internUserId, WeeklyReportCreateRequest request) {
        InternProfile intern = internProfileRepository.findByUser_Id(internUserId)
                .orElseGet(() -> internProfileRepository.findById(internUserId)
                        .orElseThrow(() -> new NotFoundException("Intern profile not found for user: " + internUserId)));

        if (intern.getMentor() == null) {
            throw new BadRequestException("Bạn chưa được phân công Mentor nên chưa thể nộp báo cáo tuần.");
        }

        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            tenantId = intern.getTenantId() != null ? intern.getTenantId() : 1L;
        }

        if (weeklyReportRepository.existsByTenantIdAndInternIdAndWeekStartDate(tenantId, intern.getId(), request.getWeekStartDate())) {
            throw new ConflictException("Báo cáo tuần cho tuần này đã tồn tại.");
        }

        WeeklyReport report = WeeklyReport.builder()
                .intern(intern)
                .mentor(intern.getMentor())
                .program(null) // Assigned if applicable
                .weekStartDate(request.getWeekStartDate())
                .weekEndDate(request.getWeekEndDate())
                .title(request.getTitle())
                .workSummary(request.getWorkSummary())
                .achievements(request.getAchievements())
                .challenges(request.getChallenges())
                .nextWeekPlan(request.getNextWeekPlan())
                .status(WeeklyReportStatus.DRAFT)
                .late(false)
                .build();
        report.setTenantId(tenantId);

        try {
            report = weeklyReportRepository.save(report);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Báo cáo tuần cho tuần này đã tồn tại.");
        }

        log.info("Created draft weekly report ID {} for intern ID {}", report.getId(), intern.getId());
        return toResponse(report);
    }

    @Override
    @Transactional
    public WeeklyReportResponse updateDraft(Long reportId, Long internUserId, WeeklyReportUpdateRequest request) {
        InternProfile intern = internProfileRepository.findByUser_Id(internUserId)
                .orElseGet(() -> internProfileRepository.findById(internUserId)
                        .orElseThrow(() -> new NotFoundException("Intern profile not found for user: " + internUserId)));

        WeeklyReport report = weeklyReportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Weekly report not found: " + reportId));

        if (!report.getIntern().getId().equals(intern.getId())) {
            throw new ForbiddenException("Bạn không có quyền chỉnh sửa báo cáo này.");
        }

        if (report.getStatus() != WeeklyReportStatus.DRAFT) {
            throw new BadRequestException("Báo cáo đã nộp không thể chỉnh sửa.");
        }

        report.setTitle(request.getTitle());
        report.setWorkSummary(request.getWorkSummary());
        report.setAchievements(request.getAchievements());
        report.setChallenges(request.getChallenges());
        report.setNextWeekPlan(request.getNextWeekPlan());

        report = weeklyReportRepository.save(report);
        log.info("Updated draft weekly report ID {}", reportId);
        return toResponse(report);
    }

    @Override
    @Transactional
    public WeeklyReportResponse submitReport(Long reportId, Long internUserId) {
        InternProfile intern = internProfileRepository.findByUser_Id(internUserId)
                .orElseGet(() -> internProfileRepository.findById(internUserId)
                        .orElseThrow(() -> new NotFoundException("Intern profile not found for user: " + internUserId)));

        WeeklyReport report = weeklyReportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Weekly report not found: " + reportId));

        if (!report.getIntern().getId().equals(intern.getId())) {
            throw new ForbiddenException("Bạn không có quyền nộp báo cáo này.");
        }

        if (report.getStatus() != WeeklyReportStatus.DRAFT) {
            throw new ConflictException("Báo cáo tuần này đã được nộp trước đó.");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = report.getWeekEndDate().atTime(23, 59, 59);

        boolean isLate = now.isAfter(deadline);
        report.setSubmittedAt(now);
        report.setLate(isLate);
        report.setStatus(isLate ? WeeklyReportStatus.LATE : WeeklyReportStatus.SUBMITTED);

        report = weeklyReportRepository.save(report);
        log.info("Submitted weekly report ID {} (isLate: {})", reportId, isLate);

        // Notify mentor
        if (report.getMentor() != null && report.getMentor().getUser() != null) {
            try {
                String internName = intern.getUser() != null ? intern.getUser().getFullName() : "Thực tập sinh";
                String statusLabel = isLate ? "Trễ hạn" : "Đúng hạn";
                notificationService.createNotification(
                        report.getMentor().getUser().getId(),
                        NotificationType.WEEKLY_REPORT,
                        "Báo cáo tuần mới (" + statusLabel + ")",
                        "Thực tập sinh " + internName + " đã nộp báo cáo tuần " + report.getTitle() + " (" + statusLabel + ")."
                );
            } catch (Exception e) {
                log.warn("Failed to send notification to mentor: {}", e.getMessage());
            }
        }

        return toResponse(report);
    }

    @Override
    @Transactional(readOnly = true)
    public WeeklyReportResponse getMyReportById(Long reportId, Long internUserId) {
        InternProfile intern = internProfileRepository.findByUser_Id(internUserId)
                .orElseGet(() -> internProfileRepository.findById(internUserId)
                        .orElseThrow(() -> new NotFoundException("Intern profile not found for user: " + internUserId)));

        WeeklyReport report = weeklyReportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("Weekly report not found: " + reportId));

        if (!report.getIntern().getId().equals(intern.getId())) {
            throw new ForbiddenException("Bạn không có quyền xem báo cáo này.");
        }

        return toResponse(report);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WeeklyReportResponse> getMyReports(Long internUserId, Pageable pageable) {
        InternProfile intern = internProfileRepository.findByUser_Id(internUserId)
                .orElseGet(() -> internProfileRepository.findById(internUserId)
                        .orElseThrow(() -> new NotFoundException("Intern profile not found for user: " + internUserId)));

        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            tenantId = intern.getTenantId() != null ? intern.getTenantId() : 1L;
        }

        return weeklyReportRepository.findByTenantIdAndInternId(tenantId, intern.getId(), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public WeeklyReportResponse getMentorReportById(Long reportId, Long mentorUserId) {
        Mentor mentor = mentorRepository.findByUser_Id(mentorUserId)
                .orElseThrow(() -> new NotFoundException("Mentor profile not found for user: " + mentorUserId));

        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            tenantId = mentor.getTenantId() != null ? mentor.getTenantId() : 1L;
        }

        WeeklyReport report = weeklyReportRepository.findByIdAndTenantIdAndMentorId(reportId, tenantId, mentor.getId())
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền xem báo cáo của thực tập sinh này."));

        return toResponse(report);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WeeklyReportResponse> getMentorReports(Long mentorUserId, WeeklyReportFilterRequest filter, Pageable pageable) {
        Mentor mentor = mentorRepository.findByUser_Id(mentorUserId)
                .orElseThrow(() -> new NotFoundException("Mentor profile not found for user: " + mentorUserId));

        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            tenantId = mentor.getTenantId() != null ? mentor.getTenantId() : 1L;
        }

        final Long finalTenantId = tenantId;
        Specification<WeeklyReport> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), finalTenantId));
            predicates.add(cb.equal(root.get("mentor").get("id"), mentor.getId()));

            if (filter.getInternId() != null) {
                predicates.add(cb.equal(root.get("intern").get("id"), filter.getInternId()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("weekStartDate"), filter.getFromDate()));
            }
            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("weekEndDate"), filter.getToDate()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return weeklyReportRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public WeeklyReportFeedbackResponse addFeedback(Long reportId, Long mentorUserId, WeeklyReportFeedbackRequest request) {
        Mentor mentor = mentorRepository.findByUser_Id(mentorUserId)
                .orElseThrow(() -> new NotFoundException("Mentor profile not found for user: " + mentorUserId));

        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            tenantId = mentor.getTenantId() != null ? mentor.getTenantId() : 1L;
        }

        WeeklyReport report = weeklyReportRepository.findByIdAndTenantIdAndMentorId(reportId, tenantId, mentor.getId())
                .orElseThrow(() -> new ForbiddenException("Bạn không có quyền phản hồi báo cáo này."));

        WeeklyReportFeedback feedback = WeeklyReportFeedback.builder()
                .weeklyReport(report)
                .mentor(mentor)
                .content(request.getContent())
                .build();
        feedback.setTenantId(tenantId);

        feedback = feedbackRepository.save(feedback);
        log.info("Mentor ID {} added feedback ID {} to weekly report ID {}", mentor.getId(), feedback.getId(), reportId);

        // Notify intern
        if (report.getIntern() != null && report.getIntern().getUser() != null) {
            try {
                String mentorName = mentor.getUser() != null ? mentor.getUser().getFullName() : "Mentor";
                notificationService.createNotification(
                        report.getIntern().getUser().getId(),
                        NotificationType.WEEKLY_REPORT,
                        "Phản hồi báo cáo tuần",
                        "Mentor " + mentorName + " đã gửi phản hồi cho báo cáo tuần của bạn."
                );
            } catch (Exception e) {
                log.warn("Failed to send notification to intern: {}", e.getMessage());
            }
        }

        return WeeklyReportFeedbackResponse.builder()
                .id(feedback.getId())
                .mentorId(mentor.getId())
                .mentorName(mentor.getUser() != null ? mentor.getUser().getFullName() : "Mentor")
                .content(feedback.getContent())
                .createdAt(feedback.getCreatedAt())
                .updatedAt(feedback.getUpdatedAt())
                .build();
    }

    private WeeklyReportResponse toResponse(WeeklyReport report) {
        List<WeeklyReportFeedbackResponse> feedbackResponses = report.getFeedbacks() != null
                ? report.getFeedbacks().stream().map(f -> WeeklyReportFeedbackResponse.builder()
                .id(f.getId())
                .mentorId(f.getMentor() != null ? f.getMentor().getId() : null)
                .mentorName(f.getMentor() != null && f.getMentor().getUser() != null ? f.getMentor().getUser().getFullName() : null)
                .content(f.getContent())
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build()).collect(Collectors.toList())
                : List.of();

        return WeeklyReportResponse.builder()
                .id(report.getId())
                .tenantId(report.getTenantId())
                .internId(report.getIntern() != null ? report.getIntern().getId() : null)
                .internName(report.getIntern() != null && report.getIntern().getUser() != null ? report.getIntern().getUser().getFullName() : null)
                .internStudentCode(report.getIntern() != null ? report.getIntern().getStudentCode() : null)
                .mentorId(report.getMentor() != null ? report.getMentor().getId() : null)
                .mentorName(report.getMentor() != null && report.getMentor().getUser() != null ? report.getMentor().getUser().getFullName() : null)
                .programId(report.getProgram() != null ? report.getProgram().getId() : null)
                .programName(report.getProgram() != null ? report.getProgram().getName() : null)
                .weekStartDate(report.getWeekStartDate())
                .weekEndDate(report.getWeekEndDate())
                .title(report.getTitle())
                .workSummary(report.getWorkSummary())
                .achievements(report.getAchievements())
                .challenges(report.getChallenges())
                .nextWeekPlan(report.getNextWeekPlan())
                .status(report.getStatus())
                .submittedAt(report.getSubmittedAt())
                .late(report.isLate())
                .feedbacks(feedbackResponses)
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
