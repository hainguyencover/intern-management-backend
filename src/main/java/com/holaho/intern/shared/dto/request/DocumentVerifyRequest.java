package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DocumentVerifyRequest {
    @NotBlank(message = "Vị trí ứng tuyển không được để trống")
    private String position;

    @Size(max = 1000)
    private String reviewNote;
}
