package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReviewWeeklyReportRequest(
                @NotBlank String feedback,
                Integer rating) {
}
