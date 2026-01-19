package com.example.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class InternProfileRequest {
    @Size(max = 255)
    private String fullName;

    @Email(message = "Invalid email format")
    private String email;

    @Pattern(regexp = "^[0-9+\\-() ]*$", message = "Invalid phone number")
    private String phone;

    @Size(max = 50)
    private String studentCode;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    @NotBlank(message = "University is required")
    @Size(max = 255)
    private String university;

    @NotBlank(message = "Major is required")
    @Size(max = 255)
    private String major;

    @Size(max = 500)
    private String address;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "4.0")
    private Double gpa;

    private LocalDate startDate;
    private LocalDate endDate;
    private Long mentorId;
    private Long userId; // For admin creation
    private String password; // Optional custom password
}
