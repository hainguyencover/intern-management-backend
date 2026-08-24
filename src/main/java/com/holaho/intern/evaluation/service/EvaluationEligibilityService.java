package com.holaho.intern.evaluation.service;

import com.holaho.intern.entity.Attendance;
import com.holaho.intern.entity.Evaluation;
import com.holaho.intern.evaluation.enums.EvaluationStatus;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.mentor.entity.MentorAssignment;
import com.holaho.intern.mentor.repository.MentorAssignmentRepository;
import com.holaho.intern.repository.AttendanceRepository;
import com.holaho.intern.repository.EvaluationRepository;
import com.holaho.intern.shared.config.TenantContext;
import com.holaho.intern.shared.enums.MentorAssignmentStatus;
import com.holaho.intern.shared.enums.TaskStatus;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.shared.exception.ForbiddenException;
import com.holaho.intern.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Validates whether a mentor is eligible to evaluate an intern.
 * Implements all business rules from BR-04 and US-019.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationEligibilityService {

    private static final double MIN_TASK_COMPLETION_RATE = 80.0;

    private final InternProfileRepository internRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final TaskRepository taskRepository;
    private final AttendanceRepository attendanceRepository;
    private final EvaluationRepository evaluationRepository;

    /**
     * Validate all eligibility rules. Throws specific exceptions on failure.
     */
    public void validateEligibility(Long internId, Long mentorUserId, String period) {
        Long tenantId = getCurrentTenantId();

        // 1. Intern must exist and have eligible status
        InternProfile intern = internRepository.findById(internId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy thực tập sinh với ID: " + internId));

        String internStatus = intern.getStatus();
        if (!"ACTIVE".equals(internStatus) && !"COMPLETED".equals(internStatus)
                && !"INTERNING".equals(internStatus)) {
            throw new BadRequestException(
                    "Thực tập sinh không đủ điều kiện đánh giá. Trạng thái hiện tại: " + internStatus);
        }

        // 2. Mentor must be assigned to this intern
        validateMentorAssignment(internId, mentorUserId);

        // 3. BR-04: Task completion >= 80%
        validateTaskCompletion(internId);

        // 4. No duplicate evaluation for same intern + period
        validateNoDuplicate(internId, period, tenantId);
    }

    /**
     * Verify that the mentor is currently assigned to this intern.
     */
    private void validateMentorAssignment(Long internId, Long mentorUserId) {
        List<MentorAssignment> assignments = mentorAssignmentRepository
                .findByIntern_IdAndStatus(internId, MentorAssignmentStatus.ACTIVE);

        boolean isMentorAssigned = assignments.stream()
                .anyMatch(a -> a.getMentor() != null
                        && a.getMentor().getUser() != null
                        && a.getMentor().getUser().getId().equals(mentorUserId));

        if (!isMentorAssigned) {
            // Also check InternProfile.mentor (legacy assignment)
            InternProfile intern = internRepository.findById(internId).orElse(null);
            if (intern == null || intern.getMentor() == null
                    || intern.getMentor().getUser() == null
                    || !intern.getMentor().getUser().getId().equals(mentorUserId)) {
                throw new ForbiddenException(
                        "Bạn không được phân công hướng dẫn thực tập sinh này. Không thể tạo đánh giá.");
            }
        }
    }

    /**
     * BR-04: Task completion rate must be >= 80%.
     */
    private void validateTaskCompletion(Long internId) {
        double completionRate = calculateTaskCompletionRate(internId);
        if (completionRate < MIN_TASK_COMPLETION_RATE) {
            throw new BadRequestException(
                    String.format("Thực tập sinh chưa đủ điều kiện đánh giá. Tỷ lệ hoàn thành công việc: %.1f%% (yêu cầu ≥ %.0f%%)",
                            completionRate, MIN_TASK_COMPLETION_RATE));
        }
    }

    /**
     * Prevent duplicate FINAL evaluations for the same intern.
     */
    private void validateNoDuplicate(Long internId, String period, Long tenantId) {
        boolean exists = evaluationRepository.existsByInternIdAndPeriod(internId, period);
        if (exists) {
            // Check if the existing one is RETURNED (can be re-created)
            Optional<Evaluation> existing = evaluationRepository
                    .findFirstByInternIdAndPeriodOrderByCreatedAtDesc(internId, period);
            if (existing.isPresent()) {
                EvaluationStatus existingStatus = existing.get().getStatus();
                if (existingStatus != null && existingStatus != EvaluationStatus.RETURNED) {
                    throw new ConflictException(
                            "Đánh giá cho thực tập sinh này trong kỳ " + period + " đã tồn tại.");
                }
            }
        }
    }

    // ── Public Calculation Methods (used by other services) ──

    /**
     * Calculate task completion rate for an intern.
     * Formula: (DONE + APPROVED tasks / total non-CANCELLED tasks) * 100
     */
    public double calculateTaskCompletionRate(Long internId) {
        long totalTasks = taskRepository.countByAssignee_Id(internId);
        if (totalTasks == 0) return 100.0; // No tasks assigned → eligible

        long completedTasks = taskRepository.countByAssignee_IdAndStatusIn(
                internId, List.of(TaskStatus.DONE, TaskStatus.APPROVED));

        return (completedTasks * 100.0) / totalTasks;
    }

    /**
     * Calculate attendance rate for an intern.
     * Formula: (PRESENT + LATE) / total * 100
     */
    public double calculateAttendanceRate(Long internId) {
        List<Attendance> attendances = attendanceRepository.findByInternId(internId);
        if (attendances.isEmpty()) return 100.0;

        long presentCount = attendances.stream()
                .filter(a -> "PRESENT".equals(a.getStatus()) || "LATE".equals(a.getStatus()))
                .count();

        return (presentCount * 100.0) / attendances.size();
    }

    private Long getCurrentTenantId() {
        Long tenantId = TenantContext.getCurrentTenantId();
        return tenantId != null ? tenantId : 1L;
    }
}
