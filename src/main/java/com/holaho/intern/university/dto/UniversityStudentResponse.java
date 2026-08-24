package com.holaho.intern.university.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UniversityStudentResponse {

    private Long id;
    private String studentCode;
    private String fullName;
    private String major;
    private String status;
    private String mentorName;
    private String programName;

    private TaskProgressInfo progress;
    private AttendanceInfo attendance;
    private EvaluationInfo evaluation;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaskProgressInfo {
        private long totalTasks;
        private long completedTasks;
        private long overdueTasks;
        private BigDecimal completionRate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttendanceInfo {
        private long workingDays;
        private long presentDays;
        private long leaveDays;
        private BigDecimal attendanceRate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EvaluationInfo {
        private BigDecimal overallScore;
        private String status;
    }
}
