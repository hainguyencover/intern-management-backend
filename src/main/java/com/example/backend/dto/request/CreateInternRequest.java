package com.example.backend.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class CreateInternRequest {
    @NotBlank @Size(max = 255)
    public String fullName;

    @NotBlank @Email @Size(max = 255)
    public String email;

    @Size(max = 20)
    public String phone;

    public LocalDate dob;

    @NotBlank @Size(max = 255)
    public String university;

    @NotBlank @Size(max = 255)
    public String major;

    @Size(max = 255)
    public String address;

    @NotNull
    public LocalDate startDate;

    @NotNull
    public LocalDate endDate;
}
