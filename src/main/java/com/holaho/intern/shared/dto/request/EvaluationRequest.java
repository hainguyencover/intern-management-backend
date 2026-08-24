package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRequest {
    @NotNull(message = "Intern ID is required")
    private Long internId;

    @NotNull(message = "Period is required")
    private String period; // MONTHLY, MIDTERM, FINAL

    @Min(0)
    @Max(10)
    private Integer score;

    @DecimalMin(value = "0.0", message = "Technical score must be at least 0")
    @DecimalMax(value = "10.0", message = "Technical score must not exceed 10")
    private Double technicalScore;

    @DecimalMin(value = "0.0", message = "Work quality score must be at least 0")
    @DecimalMax(value = "10.0", message = "Work quality score must not exceed 10")
    private Double workQualityScore;

    @DecimalMin(value = "0.0", message = "Attitude score must be at least 0")
    @DecimalMax(value = "10.0", message = "Attitude score must not exceed 10")
    private Double attitudeScore;

    @DecimalMin(value = "0.0", message = "Soft skill score must be at least 0")
    @DecimalMax(value = "10.0", message = "Soft skill score must not exceed 10")
    private Double softSkillScore;

    private String comment;
}
