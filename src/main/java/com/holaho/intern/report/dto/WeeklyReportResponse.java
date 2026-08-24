package com.holaho.intern.report.dto;

import com.holaho.intern.report.enums.WeeklyReportStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyReportResponse {

    private Long id;
    private Long tenantId;
    private Long internId;
    private String internName;
    private String internStudentCode;
    private Long mentorId;
    private String mentorName;
    private Long programId;
    private String programName;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private String title;
    private String workSummary;
    private String achievements;
    private String challenges;
    private String nextWeekPlan;
    private WeeklyReportStatus status;
    private LocalDateTime submittedAt;
    private boolean late;
    private List<WeeklyReportFeedbackResponse> feedbacks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
