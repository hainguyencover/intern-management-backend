package com.holaho.intern.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyReportCreateRequest {

    @NotNull(message = "Week start date is required")
    private LocalDate weekStartDate;

    @NotNull(message = "Week end date is required")
    private LocalDate weekEndDate;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;

    @NotBlank(message = "Work summary is required")
    @Size(max = 5000, message = "Work summary must not exceed 5000 characters")
    private String workSummary;

    @Size(max = 5000, message = "Achievements must not exceed 5000 characters")
    private String achievements;

    @Size(max = 5000, message = "Challenges must not exceed 5000 characters")
    private String challenges;

    @Size(max = 5000, message = "Next week plan must not exceed 5000 characters")
    private String nextWeekPlan;
}
