package com.holaho.intern.university.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UniversityDashboardResponse {
    private long totalStudents;
    private long interningStudents;
    private long completedStudents;
    private long terminatedStudents;
    private BigDecimal completionRate;
    private BigDecimal attendanceRate;
    private long atRiskStudents;
    private List<UniversityStudentResponse> recentAtRiskStudents;
}
