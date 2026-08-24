package com.holaho.intern.evaluation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to create a new evaluation draft.
 * Mentor provides intern + template + period.
 * Criteria scores are added later via update.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEvaluationDraftRequest {

    @NotNull(message = "Intern ID is required")
    private Long internId;

    @NotNull(message = "Template ID is required")
    private Long templateId;

    @NotNull(message = "Evaluation period is required")
    private String period; // "MIDTERM" or "FINAL"
}
