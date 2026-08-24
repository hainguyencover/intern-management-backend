package com.holaho.intern.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full evaluation response with items, scores, and status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationDetailResponse {
    private Long id;

    // Intern info
    private Long internId;
    private String internName;
    private String studentCode;

    // Mentor info
    private Long mentorId;
    private String mentorName;

    // Program info
    private Long programId;
    private String programName;

    // Template info
    private Long templateId;
    private String templateName;

    // Evaluation metadata
    private String period;
    private String status;

    // Scores
    private BigDecimal overallScore;
    private String classification;
    private String overallComment;

    // Items (scores per criterion)
    private List<EvaluationItemResponse> items;

    // Performance context
    private Double taskCompletion;
    private Double attendanceRate;
    private Integer weeklyReportCount;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime lockedAt;
    private LocalDateTime returnedAt;
    private String returnReason;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EvaluationItemResponse {
        private Long id;
        private Long criterionId;
        private String criterionName;
        private String category;
        private BigDecimal score;
        private String comment;
        private BigDecimal weight;
        private BigDecimal maxScore;
        private Integer displayOrder;
    }
}
