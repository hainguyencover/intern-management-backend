package com.example.backend.dto;

import com.example.backend.enums.ReviewDecision;

import java.time.LocalDateTime;

public record ApplicationReviewDto(
        Long id,
        Long reviewerId,
        String reviewerName,
        ReviewDecision decision,
        String comment,
        LocalDateTime decidedAt
) {}
