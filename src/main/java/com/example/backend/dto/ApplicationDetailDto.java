package com.example.backend.dto;

import com.example.backend.enums.ApplicationStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ApplicationDetailDto(
        Long id,
        Long internId,
        String internName,
        String internEmail,
        String position,
        String note,
        LocalDateTime appliedAt,
        ApplicationStatus status,
        List<ApplicationReviewDto> reviews
) {}
