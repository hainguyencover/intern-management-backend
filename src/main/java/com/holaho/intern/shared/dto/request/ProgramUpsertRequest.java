package com.holaho.intern.shared.dto.request;

import com.holaho.intern.shared.enums.ProgramStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ProgramUpsertRequest(
                @NotBlank String name,
                String description,
                @NotNull LocalDate startDate,
                @NotNull LocalDate endDate,
                ProgramStatus status // optional: nÃƒÂ¡Ã‚ÂºÃ‚Â¿u null thÃƒÆ’Ã‚Â¬ service set ACTIVE
) {
}

