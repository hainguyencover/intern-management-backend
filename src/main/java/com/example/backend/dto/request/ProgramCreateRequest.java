package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProgramCreateRequest {

    @NotNull(message = "departmentId must not be null")
    private Long departmentId;

    @NotBlank(message = "name must not be blank")
    private String name;

    private String description;

    private LocalDate startDate;
    private LocalDate endDate;
}
