package com.holaho.intern.shared.dto.request;

import com.holaho.intern.shared.enums.ReviewDecision;
import jakarta.validation.constraints.NotNull;

public record ReviewApplicationRequest(
        @NotNull ReviewDecision decision,
        String comment
) {}

