package com.holaho.intern.evaluation.service;

import com.holaho.intern.evaluation.dto.FinalReportDetailResponse;
import com.holaho.intern.evaluation.entity.FinalEvaluationReport;
import com.holaho.intern.evaluation.entity.FinalReportItem;
import com.holaho.intern.evaluation.enums.FinalReportStatus;
import com.holaho.intern.evaluation.repository.FinalEvaluationReportRepository;
import com.holaho.intern.evaluation.repository.FinalReportItemRepository;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.notification.service.NotificationService;
import com.holaho.intern.shared.config.TenantContext;
import com.holaho.intern.shared.enums.NotificationType;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service("finalReportModuleService")
@RequiredArgsConstructor
@Slf4j
public class FinalReportModuleService {

    private final FinalEvaluationReportRepository reportRepository;
    private final FinalReportItemRepository itemRepository;
    private final InternProfileRepository internRepository;
    private final FinalReportCalculationService calculationService;
    private final NotificationService notificationService;

    @Transactional
    public FinalReportDetailResponse generateReport(Long internId) {
        Long tenantId = getCurrentTenantId();

        InternProfile intern = internRepository.findById(internId)
                .orElseThrow(() -> new NotFoundException("Intern", internId));

        // Check if report already exists
        FinalEvaluationReport report = reportRepository.findByInternIdAndTenantId(internId, tenantId)
                .orElse(null);

        if (report != null && report.getStatus().isApprovedOrPublished()) {
            throw new BadRequestException("Báo cáo đánh giá tổng kết đã được phê duyệt/phát hành, không thể tạo lại.");
        }

        FinalReportCalculationService.SnapshotResult snapshot = calculationService.calculateSnapshot(intern, tenantId);

        if (report == null) {
            report = FinalEvaluationReport.builder()
                    .intern(intern)
                    .reportNumber("REP-" + System.currentTimeMillis() % 1000000)
                    .status(FinalReportStatus.DRAFT)
                    .build();
            report.setTenantId(tenantId);
        }

        report.setEvaluationScore(snapshot.evaluationScore);
        report.setTaskScore(snapshot.taskScore);
        report.setAttendanceScore(snapshot.attendanceScore);
        report.setWeeklyReportScore(snapshot.weeklyReportScore);
        report.setFinalScore(snapshot.finalScore);
        report.setClassification(snapshot.classification.name());

        report.setTaskTotal(snapshot.taskTotal);
        report.setTaskCompleted(snapshot.taskCompleted);
        report.setTaskOverdue(snapshot.taskOverdue);
        report.setTaskCompletionRate(snapshot.taskCompletionRate);

        report.setAttendanceTotal(snapshot.attendanceTotal);
        report.setAttendancePresent(snapshot.attendancePresent);
        report.setAttendanceAbsent(snapshot.attendanceAbsent);
        report.setAttendanceLate(snapshot.attendanceLate);
        report.setAttendanceRate(snapshot.attendanceRate);

        report.setReportTotal(snapshot.reportTotal);
        report.setReportSubmitted(snapshot.reportSubmitted);
        report.setReportLate(snapshot.reportLate);
        report.setReportMissing(snapshot.reportMissing);

        report.setGeneratedAt(LocalDateTime.now());

        report = reportRepository.save(report);

        // Populate report items breakdown
        itemRepository.deleteAll(report.getItems());
        List<FinalReportItem> items = new ArrayList<>();

        items.add(FinalReportItem.builder().report(report).category("SCORE").itemKey("Mentor Evaluation (60%)").score(snapshot.evaluationScore).displayOrder(1).build());
        items.add(FinalReportItem.builder().report(report).category("SCORE").itemKey("Task Performance (25%)").score(snapshot.taskScore).displayOrder(2).build());
        items.add(FinalReportItem.builder().report(report).category("SCORE").itemKey("Attendance (15%)").score(snapshot.attendanceScore).displayOrder(3).build());
        items.add(FinalReportItem.builder().report(report).category("SCORE").itemKey("Weekly Report").score(snapshot.weeklyReportScore).displayOrder(4).build());

        itemRepository.saveAll(items);
        report.setItems(items);

        log.info("Generated final report ID {} for intern {}", report.getId(), internId);
        return mapToDetailResponse(report);
    }

    @Transactional
    public FinalReportDetailResponse approveReport(Long reportId, String hrComment) {
        FinalEvaluationReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("FinalReport", reportId));

        report.setStatus(FinalReportStatus.APPROVED);
        report.setApprovedAt(LocalDateTime.now());
        if (hrComment != null) {
            report.setHrComment(hrComment);
        }

        // Auto-complete intern profile upon final report approval
        InternProfile intern = report.getIntern();
        if (intern != null && !"COMPLETED".equals(intern.getStatus())) {
            intern.setStatus("COMPLETED");
            internRepository.save(intern);
        }

        report = reportRepository.save(report);

        // Notify intern
        if (intern != null && intern.getUser() != null) {
            try {
                notificationService.createNotification(
                        intern.getUser().getId(),
                        NotificationType.SYSTEM,
                        "Báo cáo đánh giá kết quả tổng kết",
                        "Báo cáo kết quả thực tập tổng kết của bạn đã được phê duyệt. Xếp loại: " + report.getClassification());
            } catch (Exception e) {
                log.warn("Could not notify intern: {}", e.getMessage());
            }
        }

        log.info("Approved final report ID {}", reportId);
        return mapToDetailResponse(report);
    }

    @Transactional
    public FinalReportDetailResponse returnReport(Long reportId, String reason) {
        FinalEvaluationReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new NotFoundException("FinalReport", reportId));

        report.setStatus(FinalReportStatus.RETURNED);
        report.setReturnReason(reason);
        report = reportRepository.save(report);

        log.info("Returned final report ID {} for revision. Reason: {}", reportId, reason);
        return mapToDetailResponse(report);
    }

    @Transactional(readOnly = true)
    public FinalReportDetailResponse getById(Long id) {
        FinalEvaluationReport report = reportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("FinalReport", id));
        return mapToDetailResponse(report);
    }

    @Transactional(readOnly = true)
    public FinalReportDetailResponse getByIntern(Long internId) {
        Long tenantId = getCurrentTenantId();
        FinalEvaluationReport report = reportRepository.findByInternIdAndTenantId(internId, tenantId)
                .orElseThrow(() -> new NotFoundException("Báo cáo tổng kết chưa được tạo cho TTS: " + internId));
        return mapToDetailResponse(report);
    }

    @Transactional(readOnly = true)
    public Page<FinalReportDetailResponse> getAllReports(FinalReportStatus status, String keyword, Pageable pageable) {
        Long tenantId = getCurrentTenantId();
        return reportRepository.findAllWithFilters(tenantId, status, keyword, pageable)
                .map(this::mapToDetailResponse);
    }

    private FinalReportDetailResponse mapToDetailResponse(FinalEvaluationReport report) {
        List<FinalReportDetailResponse.FinalReportItemDto> itemDtos = report.getItems().stream()
                .map(item -> FinalReportDetailResponse.FinalReportItemDto.builder()
                        .id(item.getId())
                        .category(item.getCategory())
                        .itemKey(item.getItemKey())
                        .itemValue(item.getItemValue())
                        .score(item.getScore())
                        .displayOrder(item.getDisplayOrder())
                        .build())
                .toList();

        return FinalReportDetailResponse.builder()
                .id(report.getId())
                .reportNumber(report.getReportNumber())
                .internId(report.getIntern() != null ? report.getIntern().getId() : null)
                .internName(report.getIntern() != null && report.getIntern().getUser() != null ? report.getIntern().getUser().getFullName() : null)
                .studentCode(report.getIntern() != null ? report.getIntern().getStudentCode() : null)
                .email(report.getIntern() != null && report.getIntern().getUser() != null ? report.getIntern().getUser().getEmail() : null)
                .mentorId(report.getMentor() != null ? report.getMentor().getId() : null)
                .mentorName(report.getMentor() != null && report.getMentor().getUser() != null ? report.getMentor().getUser().getFullName() : null)
                .programId(report.getProgram() != null ? report.getProgram().getId() : null)
                .programName(report.getProgram() != null ? report.getProgram().getName() : null)
                .status(report.getStatus().name())
                .evaluationScore(report.getEvaluationScore())
                .taskScore(report.getTaskScore())
                .attendanceScore(report.getAttendanceScore())
                .weeklyReportScore(report.getWeeklyReportScore())
                .finalScore(report.getFinalScore())
                .classification(report.getClassification())
                .taskTotal(report.getTaskTotal())
                .taskCompleted(report.getTaskCompleted())
                .taskOverdue(report.getTaskOverdue())
                .taskCompletionRate(report.getTaskCompletionRate())
                .attendanceTotal(report.getAttendanceTotal())
                .attendancePresent(report.getAttendancePresent())
                .attendanceAbsent(report.getAttendanceAbsent())
                .attendanceLate(report.getAttendanceLate())
                .attendanceRate(report.getAttendanceRate())
                .reportTotal(report.getReportTotal())
                .reportSubmitted(report.getReportSubmitted())
                .reportLate(report.getReportLate())
                .reportMissing(report.getReportMissing())
                .mentorComment(report.getMentorComment())
                .hrComment(report.getHrComment())
                .returnReason(report.getReturnReason())
                .items(itemDtos)
                .generatedAt(report.getGeneratedAt())
                .approvedAt(report.getApprovedAt())
                .publishedAt(report.getPublishedAt())
                .build();
    }

    private Long getCurrentTenantId() {
        Long tenantId = TenantContext.getCurrentTenantId();
        return tenantId != null ? tenantId : 1L;
    }
}
