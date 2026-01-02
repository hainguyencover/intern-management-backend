package com.example.backend.dto.response;

import com.example.backend.enums.ProgramStatus;

import java.time.LocalDate;

public record ProgramResponse(
        Long id,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        ProgramStatus status
) {}
