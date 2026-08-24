package com.holaho.intern.mentor.dto;

import com.holaho.intern.shared.enums.MentorStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record UpdateMentorRequest(
        @Size(max = 150, message = "Full name must not exceed 150 characters")
        String fullName,

        @Pattern(regexp = "^[0-9+\\- ]{9,15}$", message = "Invalid phone number format")
        String phone,

        Long departmentId,
        String position,
        String title,
        String specialization,

        @PositiveOrZero(message = "Years of experience must be zero or positive")
        BigDecimal yearsOfExperience,

        @PositiveOrZero(message = "Capacity must be zero or positive")
        Integer capacity,

        MentorStatus status,
        String avatarUrl,
        String bio
) {}
