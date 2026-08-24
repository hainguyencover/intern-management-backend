package com.holaho.intern.shared.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationResponse {
    private Long id;
    private Long internId;
    private String internName;
    private Long mentorId;
    private String mentorName;
    private String period;
    private Integer score;
    private Double technicalScore;
    private Double workQualityScore;
    private Double attitudeScore;
    private Double softSkillScore;
    private Double weightedScore;
    private String resultStatus;
    private String grade;
    private String comment;
    private LocalDateTime createdAt;
}
