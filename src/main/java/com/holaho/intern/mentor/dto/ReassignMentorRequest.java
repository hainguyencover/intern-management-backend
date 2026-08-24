package com.holaho.intern.mentor.dto;

import com.holaho.intern.shared.enums.MentorAssignmentType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record ReassignMentorRequest(
        @NotNull(message = "New mentor ID is required")
        Long newMentorId,

        @NotNull(message = "Effective start date is required")
        LocalDate effectiveStartDate,

        LocalDate endDate,

        MentorAssignmentType assignmentType,

        String reason,

        String responsibility
) {}
