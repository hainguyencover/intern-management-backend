package com.holaho.intern.service;

import com.holaho.intern.shared.dto.response.FinalReportResponse;
import com.holaho.intern.entity.Attendance;
import com.holaho.intern.entity.Evaluation;
import com.holaho.intern.entity.InternProfile;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.repository.AttendanceRepository;
import com.holaho.intern.repository.EvaluationRepository;
import com.holaho.intern.repository.InternProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FinalReportService {

    private final EvaluationRepository evaluationRepository;
    private final AttendanceRepository attendanceRepository;
    private final InternProfileRepository internProfileRepository;

    @Transactional(readOnly = true)
    public FinalReportResponse calculateFinalGrade(Long internId) {
        InternProfile intern = internProfileRepository.findById(internId)
                .orElseThrow(() -> new NotFoundException("Intern not found"));

        // 1. Calculate Evaluation Score (70%)
        List<Evaluation> evaluations = evaluationRepository.findByInternId(internId);
        double avgEvalScore = evaluations.stream()
                .mapToDouble(Evaluation::getScore)
                .average()
                .orElse(0.0);

        // 2. Calculate Attendance Score (30%)
        // Rule: present > 90% = 10, > 80% = 8, else 5.
        // Or simpler: (Present Days / Total Days) * 10
        // For now, let's just count "PRESENT" vs Total recorded days.
        List<Attendance> attendances = attendanceRepository.findByInternId(internId);
        long totalDays = attendances.size();
        long presentDays = attendances.stream()
                .filter(a -> "PRESENT".equals(a.getStatus()))
                .count();

        double attendanceScore = totalDays == 0 ? 0 : ((double) presentDays / totalDays) * 10.0;

        // 3. Final Calculation
        double finalScore = (avgEvalScore * 0.7) + (attendanceScore * 0.3);

        String grade;
        if (finalScore >= 8.5)
            grade = "A";
        else if (finalScore >= 7.0)
            grade = "B";
        else if (finalScore >= 5.0)
            grade = "C";
        else
            grade = "D";

        return FinalReportResponse.builder()
                .internId(internId)
                .internName(intern.getUser().getFullName())
                .evaluationScore(Math.round(avgEvalScore * 100.0) / 100.0)
                .attendanceScore(Math.round(attendanceScore * 100.0) / 100.0)
                .finalScore(Math.round(finalScore * 100.0) / 100.0)
                .grade(grade)
                .totalEvaluations(evaluations.size())
                .totalAttendanceDays((int) totalDays)
                .build();
    }
}

