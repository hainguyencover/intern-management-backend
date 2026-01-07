package com.example.backend.dto;

import com.example.backend.enums.ApplicationStatus;

import java.time.LocalDateTime;

public record ApplicationSummaryDto(
        Long id,
        Long internId,
        String internName,
        String internEmail,
        String position,
        LocalDateTime appliedAt,
        ApplicationStatus status
) {}
