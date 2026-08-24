package com.holaho.intern.university.service;

import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.university.dto.UniversityDashboardResponse;
import com.holaho.intern.university.dto.UniversityStudentResponse;
import com.holaho.intern.university.repository.UniversityStudentRepository;
import com.holaho.intern.university.repository.projection.UniversityStudentProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
public class UniversityDashboardService {

    private final UniversityStudentRepository studentRepository;
    private final UniversityStudentService studentService;

    public UniversityDashboardResponse getDashboardData(CustomUserDetails principal) {
        Long universityId = principal != null && principal.getUniversityId() != null ? principal.getUniversityId() : 1L;
        log.info("Generating dashboard metrics for universityId: {}", universityId);

        long totalStudents = studentRepository.countTotalStudents(universityId);
        long interningStudents = studentRepository.countStudentsByStatus(universityId, "INTERNING")
                + studentRepository.countStudentsByStatus(universityId, "ACTIVE");
        long completedStudents = studentRepository.countStudentsByStatus(universityId, "COMPLETED");
        long terminatedStudents = studentRepository.countStudentsByStatus(universityId, "SUSPENDED")
                + studentRepository.countStudentsByStatus(universityId, "TERMINATED");

        BigDecimal completionRate = totalStudents > 0
                ? BigDecimal.valueOf((completedStudents * 100.0) / totalStudents).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Page<UniversityStudentProjection> allStudents = studentRepository.searchStudents(
                universityId, null, null, null, Pageable.unpaged()
        );

        List<UniversityStudentResponse> studentResponses = allStudents.getContent().stream()
                .map(studentService::mapProjectionToResponse)
                .collect(Collectors.toList());

        BigDecimal totalAttendance = studentResponses.stream()
                .map(s -> s.getAttendance().getAttendanceRate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageAttendance = studentResponses.isEmpty()
                ? BigDecimal.valueOf(100.00)
                : totalAttendance.divide(BigDecimal.valueOf(studentResponses.size()), 2, RoundingMode.HALF_UP);

        // At-Risk criteria: completionRate < 50% OR attendanceRate < 80% OR overdueTasks >= 3
        List<UniversityStudentResponse> atRiskStudents = studentResponses.stream()
                .filter(s -> (s.getProgress().getCompletionRate().doubleValue() < 50.0)
                        || (s.getAttendance().getAttendanceRate().doubleValue() < 80.0)
                        || (s.getProgress().getOverdueTasks() >= 3))
                .collect(Collectors.toList());

        return UniversityDashboardResponse.builder()
                .totalStudents(totalStudents)
                .interningStudents(interningStudents)
                .completedStudents(completedStudents)
                .terminatedStudents(terminatedStudents)
                .completionRate(completionRate)
                .attendanceRate(averageAttendance)
                .atRiskStudents(atRiskStudents.size())
                .recentAtRiskStudents(atRiskStudents.stream().limit(10).collect(Collectors.toList()))
                .build();
    }
}
