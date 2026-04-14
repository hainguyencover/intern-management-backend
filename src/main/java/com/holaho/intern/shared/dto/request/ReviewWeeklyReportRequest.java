package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReviewWeeklyReportRequest(
                @NotBlank String feedback,
                Integer rating) {
}

