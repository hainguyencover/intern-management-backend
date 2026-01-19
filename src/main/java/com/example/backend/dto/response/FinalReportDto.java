package com.example.backend.dto.response;

import com.example.backend.dto.WeeklyReportDto;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class FinalReportDto {
    // Intern Info
    private Long internId;
    private String fullName;
    private String studentCode;
    private String university;
    private String major;
    private String email;

    // Period
    private LocalDate startDate;
    private LocalDate endDate;

    // Mentor/Group
    private String mentorName;
    private String groupName; // Program Group Name

    // Aggregated Data
    private List<EvaluationResponse> evaluations;
    private List<WeeklyReportDto> weeklyReports;

    // Summary
    private Double finalScore; // Average score
    private String finalAssessment; // Excellent, Good, etc.
    private Integer totalReports;
}
