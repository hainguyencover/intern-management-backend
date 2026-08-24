package com.holaho.intern.evaluation.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request to score a single evaluation criterion.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationCriterionScoreRequest {

    @NotNull(message = "Criterion ID is required")
    private Long criterionId;

    @NotNull(message = "Score is required")
    @DecimalMin(value = "0.0", message = "Score must be at least 0")
    @DecimalMax(value = "10.0", message = "Score must not exceed 10")
    private BigDecimal score;

    @Size(max = 2000, message = "Comment must not exceed 2000 characters")
    private String comment;
}
