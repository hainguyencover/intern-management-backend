package com.holaho.intern.mentor.dto;

import com.holaho.intern.shared.enums.MentorStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record MentorStatusUpdateRequest(
        @NotNull(message = "Status is required")
        MentorStatus status,

        String reason,

        boolean forceDeactivate
) {}
