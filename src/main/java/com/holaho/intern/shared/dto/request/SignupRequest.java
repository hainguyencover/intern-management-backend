package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, max = 64)
    private String password;

    @NotBlank
    @Size(min = 8, max = 20)
    private String phone;

    @NotNull
    @Min(1900)
    @Max(2100)
    private Integer dobYear; // nÃ„Æ’m sinh

    @NotBlank
    private String address;

    @NotBlank
    private String studentCode;

    @NotBlank
    private String university;

    @NotBlank
    private String major;

    @NotNull
    @Min(1900)
    @Max(2100)
    private Integer startYear;

    @NotNull
    @Min(1900)
    @Max(2100)
    private Integer endYear;
}

