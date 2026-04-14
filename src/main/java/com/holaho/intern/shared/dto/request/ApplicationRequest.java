package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApplicationRequest {
    @NotBlank(message = "Position is required")
    @Size(max = 255)
    private String position;

    @Size(max = 1000)
    private String note;
}

