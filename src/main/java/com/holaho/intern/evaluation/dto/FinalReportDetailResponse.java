package com.holaho.intern.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalReportDetailResponse {
    private Long id;
    private String reportNumber;

    // Intern Info
    private Long internId;
    private String internName;
    private String studentCode;
    private String email;

    // Program & Mentor Info
    private Long programId;
    private String programName;
    private Long mentorId;
    private String mentorName;

    // Status
    private String status;

    // Scores
    private BigDecimal evaluationScore;
    private BigDecimal taskScore;
    private BigDecimal attendanceScore;
    private BigDecimal weeklyReportScore;
    private BigDecimal finalScore;
    private String classification;

    // Performance Stats
    private Integer taskTotal;
    private Integer taskCompleted;
    private Integer taskOverdue;
    private BigDecimal taskCompletionRate;

    private Integer attendanceTotal;
    private Integer attendancePresent;
    private Integer attendanceAbsent;
    private Integer attendanceLate;
    private BigDecimal attendanceRate;

    private Integer reportTotal;
    private Integer reportSubmitted;
    private Integer reportLate;
    private Integer reportMissing;

    // Comments
    private String mentorComment;
    private String hrComment;
    private String returnReason;

    // Items
    private List<FinalReportItemDto> items;

    // Timestamps
    private LocalDateTime generatedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime publishedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FinalReportItemDto {
        private Long id;
        private String category;
        private String itemKey;
        private String itemValue;
        private BigDecimal score;
        private Integer displayOrder;
    }
}
