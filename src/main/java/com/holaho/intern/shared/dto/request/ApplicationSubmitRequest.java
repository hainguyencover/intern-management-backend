package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApplicationSubmitRequest {
    @NotBlank(message = "Vị trí ứng tuyển không được để trống")
    private String position;

    @NotNull(message = "ID chương trình không được để trống")
    private Long programId;

    private String note;
}
