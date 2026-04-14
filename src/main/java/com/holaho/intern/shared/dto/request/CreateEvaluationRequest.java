package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateEvaluationRequest {

    @NotNull(message = "Intern ID is required")
    private Long internId;

    @Size(max = 50)
    private String period;

    @NotNull(message = "Score is required")
    @Min(value = 0, message = "Score must be at least 0")
    @Max(value = 100, message = "Score must be at most 100")
    private Integer score;

    @Size(max = 2000)
    private String comment;
}

