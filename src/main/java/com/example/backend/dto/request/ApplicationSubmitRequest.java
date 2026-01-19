package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApplicationSubmitRequest {
    @NotBlank(message = "Vị trí ứng tuyển không được để trống")
    private String position;

    private String note;
}
