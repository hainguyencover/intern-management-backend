package com.holaho.intern.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary of an intern pending evaluation (for mentor's pending list).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingEvaluationResponse {
    private Long internId;
    private String internName;
    private String studentCode;
    private String programName;
    private Double taskCompletion;
    private Double attendanceRate;
    private Integer weeklyReportCount;
    private String evaluationStatus; // PENDING, DRAFT, etc.
    private Long draftEvaluationId;  // null if no draft yet
}
