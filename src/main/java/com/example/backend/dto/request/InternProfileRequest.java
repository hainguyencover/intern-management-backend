package com.example.backend.dto.request;

import lombok.*;
import jakarta.validation.constraints.*;


import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternProfileRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "Phone number must be 10-11 digits")
    private String phone;

    private LocalDate dob;

    @NotBlank(message = "Student code is required")
    private String studentCode;

    @NotBlank(message = "University is required")
    private String university;

    @NotBlank(message = "Major is required")
    private String major;

    private String address;

    private Double gpa;

    private String cvUrl;

    private LocalDate startDate;

    private LocalDate endDate;
}
