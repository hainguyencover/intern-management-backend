package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LeaveRequestRejectRequest {
    @NotBlank
    @Size(max = 1000)
    private String reason;
}

