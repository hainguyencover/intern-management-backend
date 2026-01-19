package com.example.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveRequestCreateRequest {
    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotBlank
    @NotBlank
    @Size(max = 2000)
    private String reason;

    @NotNull
    private com.example.backend.enums.LeaveType leaveType;
}
