package com.example.backend.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class FinalEvaluationReportResponse {
    private String period;

    private Integer totalInterns;
    private Integer totalEvaluations;

    private Double avgSkillScore;
    private Double avgAttitudeScore;
    private Double avgOverallScore;

    private List<FinalEvaluationRowResponse> rows;
}

