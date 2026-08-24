package com.holaho.intern.evaluation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request to update an existing evaluation draft with scores and comments.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEvaluationRequest {

    @Valid
    private List<EvaluationCriterionScoreRequest> criteria;

    @Size(max = 4000, message = "Overall comment must not exceed 4000 characters")
    private String overallComment;
}
