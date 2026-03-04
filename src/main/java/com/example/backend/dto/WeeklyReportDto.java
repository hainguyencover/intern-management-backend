package com.example.backend.dto;

import com.example.backend.enums.WeeklyReportStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record WeeklyReportDto(
                Long id,
                Long internId,
                String internName,
                Integer weekNumber,
                LocalDate weekStart,
                LocalDate weekEnd,
                LocalDate reportDate,
                String completedWork,
                String plannedWork,
                String challenges,
                String learnings,
                String summary,
                WeeklyReportStatus status,
                String mentorFeedback,
                Integer rating,
                Long mentorId,
                String mentorName,
                LocalDateTime createdAt,
                LocalDateTime reviewedAt,
                String sentimentLabel,
                Double sentimentScore) {
}
