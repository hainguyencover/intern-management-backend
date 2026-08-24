package com.holaho.intern.mentor.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CreateMentorRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name must not exceed 150 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @Pattern(regexp = "^[0-9+\\- ]{9,15}$", message = "Invalid phone number format")
        String phone,

        @NotBlank(message = "Employee code is required")
        @Size(max = 50, message = "Employee code must not exceed 50 characters")
        String employeeCode,

        Long departmentId,
        String position,
        String title,
        String specialization,

        @PositiveOrZero(message = "Years of experience must be zero or positive")
        BigDecimal yearsOfExperience,

        @PositiveOrZero(message = "Capacity must be zero or positive")
        Integer capacity,

        String avatarUrl,
        String bio,

        Long userId,
        String initialPassword
) {}
