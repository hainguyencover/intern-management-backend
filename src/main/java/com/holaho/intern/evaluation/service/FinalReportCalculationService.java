package com.holaho.intern.evaluation.service;

import com.holaho.intern.entity.Attendance;
import com.holaho.intern.evaluation.entity.Evaluation;
import com.holaho.intern.evaluation.enums.EvaluationClassification;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.repository.AttendanceRepository;
import com.holaho.intern.repository.EvaluationRepository;
import com.holaho.intern.report.repository.WeeklyReportRepository;
import com.holaho.intern.shared.enums.TaskStatus;
import com.holaho.intern.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Calculates snapshot scores and stats for Final Evaluation Reports.
 *
 * Final Score Weights:
 *   - Evaluation Score (Mentor rubric): 60%
 *   - Task Completion Score: 25%
 *   - Attendance Score: 15%
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FinalReportCalculationService {

    private final EvaluationRepository evaluationRepository;
    private final TaskRepository taskRepository;
    private final AttendanceRepository attendanceRepository;
    private final WeeklyReportRepository weeklyReportRepository;

    public static class SnapshotResult {
        public BigDecimal evaluationScore;
        public BigDecimal taskScore;
        public BigDecimal attendanceScore;
        public BigDecimal weeklyReportScore;
        public BigDecimal finalScore;
        public EvaluationClassification classification;

        public int taskTotal;
        public int taskCompleted;
        public int taskOverdue;
        public BigDecimal taskCompletionRate;

        public int attendanceTotal;
        public int attendancePresent;
        public int attendanceAbsent;
        public int attendanceLate;
        public BigDecimal attendanceRate;

        public int reportTotal;
        public int reportSubmitted;
        public int reportLate;
        public int reportMissing;
    }

    public SnapshotResult calculateSnapshot(InternProfile intern, Long tenantId) {
        SnapshotResult res = new SnapshotResult();
        Long internId = intern.getId();

        // 1. Evaluation Score (60%)
        List<Evaluation> evals = evaluationRepository.findByInternId(internId);
        if (!evals.isEmpty()) {
            double avgEvalScore = evals.stream()
                    .filter(e -> e.getOverallScore() != null || e.getWeightedScore() != null)
                    .mapToDouble(e -> e.getOverallScore() != null ? e.getOverallScore().doubleValue() : e.getWeightedScore())
                    .average()
                    .orElse(7.0);
            res.evaluationScore = BigDecimal.valueOf(avgEvalScore).setScale(2, RoundingMode.HALF_UP);
        } else {
            res.evaluationScore = new BigDecimal("7.00");
        }

        // 2. Task Performance (25%)
        long totalTasks = taskRepository.countByAssignee_Id(internId);
        long completedTasks = taskRepository.countByAssignee_IdAndStatusIn(
                internId, List.of(TaskStatus.DONE, TaskStatus.APPROVED));

        res.taskTotal = (int) totalTasks;
        res.taskCompleted = (int) completedTasks;
        res.taskOverdue = 0; // default
        double taskRate = totalTasks == 0 ? 100.0 : ((double) completedTasks / totalTasks) * 100.0;
        res.taskCompletionRate = BigDecimal.valueOf(taskRate).setScale(2, RoundingMode.HALF_UP);
        res.taskScore = BigDecimal.valueOf((taskRate / 100.0) * 10.0).setScale(2, RoundingMode.HALF_UP);

        // 3. Attendance Performance (15%)
        List<Attendance> attendances = attendanceRepository.findByInternId(internId);
        res.attendanceTotal = attendances.size();
        long presentCount = attendances.stream().filter(a -> "PRESENT".equals(a.getStatus())).count();
        long lateCount = attendances.stream().filter(a -> "LATE".equals(a.getStatus())).count();
        long absentCount = attendances.stream().filter(a -> "ABSENT".equals(a.getStatus())).count();

        res.attendancePresent = (int) presentCount;
        res.attendanceLate = (int) lateCount;
        res.attendanceAbsent = (int) absentCount;

        double attRate = attendances.isEmpty() ? 100.0 : (((double) presentCount + (lateCount * 0.5)) / attendances.size()) * 100.0;
        res.attendanceRate = BigDecimal.valueOf(attRate).setScale(2, RoundingMode.HALF_UP);
        res.attendanceScore = BigDecimal.valueOf((attRate / 100.0) * 10.0).setScale(2, RoundingMode.HALF_UP);

        // 4. Weekly Report Stats
        long reportCount = weeklyReportRepository.countByTenantIdAndInternId(tenantId, internId);
        res.reportTotal = (int) reportCount;
        res.reportSubmitted = (int) reportCount;
        res.reportLate = 0;
        res.reportMissing = 0;
        res.weeklyReportScore = BigDecimal.valueOf(Math.min(10.0, reportCount * 2.0)).setScale(2, RoundingMode.HALF_UP);

        // 5. Weighted Final Score: 60% Eval + 25% Task + 15% Attendance
        double finalVal = (res.evaluationScore.doubleValue() * 0.60)
                + (res.taskScore.doubleValue() * 0.25)
                + (res.attendanceScore.doubleValue() * 0.15);

        res.finalScore = BigDecimal.valueOf(finalVal).setScale(2, RoundingMode.HALF_UP);
        res.classification = EvaluationClassification.fromScore(res.finalScore.doubleValue());

        return res;
    }
}
