package com.holaho.intern.shared.dto;

import com.holaho.intern.shared.enums.ReviewDecision;

import java.time.LocalDateTime;

public record ApplicationReviewDto(
        Long id,
        Long reviewerId,
        String reviewerName,
        ReviewDecision decision,
        String comment,
        LocalDateTime decidedAt
) {}

