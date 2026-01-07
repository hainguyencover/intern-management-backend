package com.example.backend.dto.request;

import com.example.backend.enums.ReviewDecision;
import jakarta.validation.constraints.NotNull;

public record ReviewApplicationRequest(
        @NotNull ReviewDecision decision,
        String comment
) {}
