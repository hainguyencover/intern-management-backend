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
public class EvaluationTemplateResponse {
    private Long id;
    private Long programId;
    private String programName;
    private String name;
    private String description;
    private String evaluationPeriod;
    private Integer version;
    private String status;
    private List<CriterionResponse> criteria;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CriterionResponse {
        private Long id;
        private String category;
        private String name;
        private String description;
        private BigDecimal weight;
        private BigDecimal maxScore;
        private Integer displayOrder;
        private Boolean required;
    }
}
