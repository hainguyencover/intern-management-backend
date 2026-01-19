package com.example.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FinalReportResponse {
    private Long internId;
    private String internName;
    private Double evaluationScore;
    private Double attendanceScore;
    private Double finalScore;
    private String grade;
    private Integer totalEvaluations;
    private Integer totalAttendanceDays;
}
