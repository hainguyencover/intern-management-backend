package com.holaho.intern.mentor.dto;

import com.holaho.intern.shared.enums.MentorAssignmentType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record AssignMentorRequest(
        @NotNull(message = "Mentor ID is required")
        Long mentorId,

        @NotNull(message = "Intern ID is required")
        Long internId,

        Long enrollmentId,

        MentorAssignmentType assignmentType,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        LocalDate endDate,

        String responsibility
) {}
