package com.holaho.intern.shared.dto;

import com.holaho.intern.shared.enums.ApplicationStatus;

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

