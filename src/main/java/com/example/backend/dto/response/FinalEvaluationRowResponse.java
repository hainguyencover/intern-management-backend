package com.example.backend.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FinalEvaluationRowResponse {
    private Long internId;
    private String studentCode;
    private String internName;
    private String university;
    private String major;
    private Double gpa;

    private Long mentorId;
    private String mentorName;

    private Integer skillScore;
    private Integer attitudeScore;
    private Integer overallScore;
    private String comment;

    private String period;
}

