package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApplicationRequest {
    @NotBlank
    private String position;

    private String note;
}
