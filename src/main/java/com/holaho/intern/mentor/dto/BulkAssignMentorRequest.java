package com.holaho.intern.mentor.dto;

import com.holaho.intern.shared.enums.MentorAssignmentType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record BulkAssignMentorRequest(
        @NotNull(message = "Mentor ID is required")
        Long mentorId,

        @NotEmpty(message = "At least one intern ID must be provided")
        List<Long> internIds,

        MentorAssignmentType assignmentType,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        LocalDate endDate,

        String responsibility
) {}
