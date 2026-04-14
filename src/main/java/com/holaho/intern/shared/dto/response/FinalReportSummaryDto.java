package com.holaho.intern.shared.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FinalReportSummaryDto {
    private Long internId;
    private String fullName;
    private String studentCode;
    private String university;
    private String mentorName;
    private Double finalScore;
    private String finalAssessment;
    private Integer reportCount;
}

