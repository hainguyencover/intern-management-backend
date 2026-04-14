package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EvaluationRequest {
    @NotNull
    private Long internId;

    @NotNull
    private String period; // WEEKLY, MIDTERM, FINAL

    @Min(0)
    @Max(10)
    private Integer score;

    private String comment;
}

