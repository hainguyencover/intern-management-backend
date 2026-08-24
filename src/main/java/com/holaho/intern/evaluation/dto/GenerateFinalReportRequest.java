package com.holaho.intern.evaluation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateFinalReportRequest {
    @NotNull(message = "Intern ID is required")
    private Long internId;
}
