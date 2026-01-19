package com.example.backend.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInternProfileRequest {

    @Size(max = 50)
    private String studentCode;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dob;

    @Size(max = 255)
    private String university;

    @Size(max = 255)
    private String major;

    @Size(max = 20)
    private String phone;

    @Size(max = 500)
    private String address;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "4.0")
    private Double gpa;

    private LocalDate startDate;

    private LocalDate endDate;

    private Long mentorId;
}
