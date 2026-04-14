package com.holaho.intern.shared.dto.request;

import com.holaho.intern.shared.enums.LeaveType;


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
    private com.holaho.intern.shared.enums.LeaveType leaveType;
}

