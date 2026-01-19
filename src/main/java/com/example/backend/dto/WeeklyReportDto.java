package com.example.backend.dto;

import com.example.backend.enums.WeeklyReportStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WeeklyReportDto(
                Long id,
                Long internId,
                String internName,
                Integer weekNumber,
                LocalDate reportDate,
                String completedWork,
                String plannedWork,
                String challenges,
                String learnings,
                WeeklyReportStatus status,
                String mentorFeedback,
                Long mentorId,
                String mentorName,
                LocalDateTime createdAt,
                LocalDateTime reviewedAt) {
}
