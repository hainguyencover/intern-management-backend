package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class WeeklyReportRequest {
    @NotNull
    private Integer weekNumber;

    @NotNull
    private LocalDate reportDate;

    @NotNull
    private LocalDate weekStart;

    @NotNull
    private LocalDate weekEnd;

    @NotBlank(message = "Completed work is required")
    private String completedWork;

    private String plannedWork;
    private String challenges;
    private String learnings;
}
