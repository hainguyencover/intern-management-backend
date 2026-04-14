package com.holaho.intern.shared.dto.request;

import com.holaho.intern.entity.Department;
import com.holaho.intern.entity.Program;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateProgramRequest {

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    @NotBlank(message = "Program name is required")
    private String name;

    private String description;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;
}

