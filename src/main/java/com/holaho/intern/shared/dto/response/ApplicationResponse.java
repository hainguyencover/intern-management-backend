package com.holaho.intern.shared.dto.response;

import com.holaho.intern.shared.enums.ReviewDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {
    private Long id;
    private Long internId;
    private String internName;
    private String internEmail;
    private String position;
    private LocalDateTime appliedAt;
    private String status;
    private String note;
    private List<ApplicationReviewResponse> reviews;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // AI Screening Fields
    private Integer aiScore;
    private List<String> aiSkills;
    private String aiSummary;
    private String aiRecommendation;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApplicationReviewResponse {
        private Long id;
        private Long applicationId;
        private Long reviewerId;
        private String reviewerName;
        private ReviewDecision decision;
        private String comment;
        private LocalDateTime decidedAt;
    }
}
