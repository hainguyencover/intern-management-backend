package com.example.backend.dto.request;

import com.example.backend.enums.ProgramStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ProgramUpsertRequest(
        @NotBlank String name,
        String description,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        ProgramStatus status // optional: nếu null thì service set DRAFT
) {}
