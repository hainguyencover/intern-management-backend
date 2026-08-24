package com.holaho.intern.service;

import com.holaho.intern.entity.Evaluation;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.notification.service.NotificationService;
import com.holaho.intern.repository.AttendanceRepository;
import com.holaho.intern.repository.EvaluationRepository;
import com.holaho.intern.shared.config.TenantContext;
import com.holaho.intern.shared.dto.request.EvaluationRequest;
import com.holaho.intern.shared.dto.response.EvaluationResponse;
import com.holaho.intern.shared.enums.NotificationType;
import com.holaho.intern.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final InternProfileRepository internRepository;
    private final MentorRepository mentorRepository;
    private final AttendanceRepository attendanceRepository;
    private final NotificationService notificationService;

    @Transactional
    public EvaluationResponse create(EvaluationRequest req, Long mentorUserId) {
        InternProfile intern = internRepository.findById(req.getInternId())
                .orElseThrow(() -> new NotFoundException("Intern not found: " + req.getInternId()));

        Mentor mentor = mentorRepository.findByUser_Id(mentorUserId)
                .orElseThrow(() -> new NotFoundException("Mentor profile not found for user: " + mentorUserId));

        double baseScore = req.getScore() != null ? req.getScore().doubleValue() : 7.0;
        double tech = req.getTechnicalScore() != null ? req.getTechnicalScore() : baseScore;
        double quality = req.getWorkQualityScore() != null ? req.getWorkQualityScore() : baseScore;
        double attitude = req.getAttitudeScore() != null ? req.getAttitudeScore() : baseScore;
        double soft = req.getSoftSkillScore() != null ? req.getSoftSkillScore() : baseScore;

        double weighted = Math.round((0.35 * tech + 0.30 * quality + 0.20 * attitude + 0.15 * soft) * 100.0) / 100.0;
        String passFail = weighted >= 6.5 ? "PASS" : "FAIL";

        Long tenantId = TenantContext.getCurrentTenantId();
        if (tenantId == null) {
            tenantId = mentor.getTenantId() != null ? mentor.getTenantId() : 1L;
        }

        Evaluation eval = Evaluation.builder()
                .intern(intern)
                .mentor(mentor)
                .period(req.getPeriod())
                .score(req.getScore() != null ? req.getScore() : (int) Math.round(weighted))
                .technicalScore(tech)
                .workQualityScore(quality)
                .attitudeScore(attitude)
                .softSkillScore(soft)
                .weightedScore(weighted)
                .resultStatus(passFail)
                .comment(req.getComment())
                .build();
        eval.setTenantId(tenantId);

        eval = evaluationRepository.save(eval);
        log.info("Created evaluation ID {} for intern ID {} with weighted score {}", eval.getId(), intern.getId(), weighted);

        // BR-04: Auto-transition to COMPLETED if weightedScore >= 6.5 AND FINAL period
        if (weighted >= 6.5 && "FINAL".equalsIgnoreCase(req.getPeriod())) {
            long totalAttendance = attendanceRepository.findByInternId(intern.getId()).size();
            log.info("Intern {} attendance entries count: {}", intern.getId(), totalAttendance);
            intern.setStatus("COMPLETED");
            internRepository.save(intern);
            log.info("Auto-completed intern profile {} upon meeting BR-04 final evaluation grade: {}", intern.getId(), weighted);
        }

        // Notify intern
        if (intern.getUser() != null) {
            try {
                String mentorName = mentor.getUser() != null ? mentor.getUser().getFullName() : "Mentor";
                notificationService.createNotification(
                        intern.getUser().getId(),
                        NotificationType.SYSTEM,
                        "Đánh giá kết quả thực tập",
                        "Mentor " + mentorName + " đã hoàn thành kết quả đánh giá (" + req.getPeriod() + ") của bạn. Điểm tổng kết: " + weighted
                );
            } catch (Exception e) {
                log.warn("Could not dispatch evaluation notification to intern: {}", e.getMessage());
            }
        }

        return mapToResponse(eval);
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> getByIntern(Long internId) {
        return evaluationRepository.findByInternId(internId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public EvaluationResponse getById(Long id) {
        Evaluation eval = evaluationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Evaluation not found: " + id));
        return mapToResponse(eval);
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> getInternEvaluations(Long userId) {
        InternProfile intern = internRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Intern profile not found for user: " + userId));
        return evaluationRepository.findByInternId(intern.getId()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<EvaluationResponse> getMentorEvaluations(Long userId, String period, String keyword, Pageable pageable) {
        Mentor mentor = mentorRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Mentor profile not found for user: " + userId));
        return evaluationRepository.findByMentorIdAndFilters(
                mentor.getId(),
                period != null && !period.isBlank() ? period.trim() : null,
                keyword != null && !keyword.isBlank() ? keyword.trim() : null,
                pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<EvaluationResponse> getAllEvaluations(String period, String keyword, Pageable pageable) {
        return evaluationRepository.findAllEvaluationsWithFilters(
                period != null && !period.isBlank() ? period.trim() : null,
                keyword != null && !keyword.isBlank() ? keyword.trim() : null,
                pageable)
                .map(this::mapToResponse);
    }

    private EvaluationResponse mapToResponse(Evaluation eval) {
        double w = eval.getWeightedScore() != null ? eval.getWeightedScore() : (eval.getScore() != null ? eval.getScore().doubleValue() : 0.0);
        String grade;
        if (w >= 9.0) grade = "A";
        else if (w >= 8.0) grade = "B";
        else if (w >= 6.5) grade = "C";
        else grade = "D";

        return EvaluationResponse.builder()
                .id(eval.getId())
                .internId(eval.getIntern() != null ? eval.getIntern().getId() : null)
                .internName(eval.getIntern() != null && eval.getIntern().getUser() != null ? eval.getIntern().getUser().getFullName() : null)
                .mentorId(eval.getMentor() != null ? eval.getMentor().getId() : null)
                .mentorName(eval.getMentor() != null && eval.getMentor().getUser() != null ? eval.getMentor().getUser().getFullName() : null)
                .period(eval.getPeriod())
                .score(eval.getScore())
                .technicalScore(eval.getTechnicalScore())
                .workQualityScore(eval.getWorkQualityScore())
                .attitudeScore(eval.getAttitudeScore())
                .softSkillScore(eval.getSoftSkillScore())
                .weightedScore(w)
                .resultStatus(eval.getResultStatus() != null ? eval.getResultStatus() : (w >= 6.5 ? "PASS" : "FAIL"))
                .grade(grade)
                .comment(eval.getComment())
                .createdAt(eval.getCreatedAt())
                .build();
    }
}
