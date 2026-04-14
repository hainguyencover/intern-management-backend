package com.holaho.intern.shared.dto.request;

import com.holaho.intern.user.entity.User;


import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateInternProfileRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @Size(max = 50)
    private String studentCode;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    @NotBlank(message = "{intern.university.required}")
    @Size(max = 255)
    private String university;

    @NotBlank(message = "{intern.major.required}")
    @Size(max = 255)
    private String major;

    @Size(max = 20)
    private String phone;

    @Size(max = 500)
    private String address;

    @DecimalMin(value = "0.0", message = "{intern.gpa.min}")
    @DecimalMax(value = "4.0", message = "{intern.gpa.max}")
    private Double gpa;

    private LocalDate startDate;

    private LocalDate endDate;

    private Long mentorId;
}

