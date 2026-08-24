package com.holaho.intern.university.service;

import com.holaho.intern.shared.exception.ResourceNotFoundException;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.university.dto.UniversityStudentFilter;
import com.holaho.intern.university.dto.UniversityStudentResponse;
import com.holaho.intern.university.repository.UniversityStudentRepository;
import com.holaho.intern.university.repository.projection.UniversityStudentProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UniversityStudentService {

    private final UniversityStudentRepository studentRepository;

    public Page<UniversityStudentResponse> findStudents(
            CustomUserDetails principal,
            UniversityStudentFilter filter,
            Pageable pageable
    ) {
        Long universityId = getEffectiveUniversityId(principal);
        log.info("Fetching university students for universityId: {}, filter: {}", universityId, filter);

        Page<UniversityStudentProjection> projections = studentRepository.searchStudents(
                universityId,
                filter.getKeyword(),
                filter.getStatus(),
                filter.getMajor(),
                pageable
        );

        List<UniversityStudentResponse> responses = projections.getContent().stream()
                .map(this::mapProjectionToResponse)
                .filter(res -> filterProgressRange(res, filter.getProgressMin(), filter.getProgressMax()))
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, projections.getTotalElements());
    }

    public UniversityStudentResponse getStudentDetail(
            CustomUserDetails principal,
            Long studentId
    ) {
        Long universityId = getEffectiveUniversityId(principal);
        log.info("Fetching student detail studentId: {} for universityId: {}", studentId, universityId);

        // Scope Isolation: Check student belongs to principal's university
        studentRepository.findByIdAndUniversityId(studentId, universityId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        Page<UniversityStudentProjection> singlePage = studentRepository.searchStudents(
                universityId,
                null,
                null,
                null,
                Pageable.unpaged()
        );

        return singlePage.getContent().stream()
                .filter(proj -> proj.getId().equals(studentId))
                .findFirst()
                .map(this::mapProjectionToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Student detail projection not found for id: " + studentId));
    }

    private Long getEffectiveUniversityId(CustomUserDetails principal) {
        if (principal == null || principal.getUniversityId() == null) {
            // Default to 1 (FPT University) for testing/fallback if role is ADMIN without specific university mapping
            return 1L;
        }
        return principal.getUniversityId();
    }

    public UniversityStudentResponse mapProjectionToResponse(UniversityStudentProjection proj) {
        long totalTasks = proj.getTotalTasks() != null ? proj.getTotalTasks() : 0L;
        long completedTasks = proj.getCompletedTasks() != null ? proj.getCompletedTasks() : 0L;
        long overdueTasks = proj.getOverdueTasks() != null ? proj.getOverdueTasks() : 0L;

        BigDecimal completionRate = totalTasks > 0
                ? BigDecimal.valueOf((completedTasks * 100.0) / totalTasks).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        long workingDays = proj.getWorkingDays() != null ? proj.getWorkingDays() : 0L;
        long presentDays = proj.getPresentDays() != null ? proj.getPresentDays() : 0L;
        long leaveDays = proj.getLeaveDays() != null ? proj.getLeaveDays() : 0L;
        BigDecimal attendanceRate = proj.getAttendanceRate() != null ? proj.getAttendanceRate() : BigDecimal.valueOf(100.00);

        return UniversityStudentResponse.builder()
                .id(proj.getId())
                .studentCode(proj.getStudentCode())
                .fullName(proj.getFullName())
                .major(proj.getMajor())
                .status(proj.getStatus())
                .mentorName(proj.getMentorName())
                .programName(proj.getProgramName())
                .progress(UniversityStudentResponse.TaskProgressInfo.builder()
                        .totalTasks(totalTasks)
                        .completedTasks(completedTasks)
                        .overdueTasks(overdueTasks)
                        .completionRate(completionRate)
                        .build())
                .attendance(UniversityStudentResponse.AttendanceInfo.builder()
                        .workingDays(workingDays)
                        .presentDays(presentDays)
                        .leaveDays(leaveDays)
                        .attendanceRate(attendanceRate)
                        .build())
                .evaluation(UniversityStudentResponse.EvaluationInfo.builder()
                        .overallScore(proj.getOverallScore())
                        .status(proj.getEvaluationStatus() != null ? proj.getEvaluationStatus() : "PENDING")
                        .build())
                .build();
    }

    private boolean filterProgressRange(UniversityStudentResponse res, Double min, Double max) {
        if (min == null && max == null) return true;
        double rate = res.getProgress() != null && res.getProgress().getCompletionRate() != null
                ? res.getProgress().getCompletionRate().doubleValue()
                : 0.0;
        if (min != null && rate < min) return false;
        if (max != null && rate > max) return false;
        return true;
    }
}
