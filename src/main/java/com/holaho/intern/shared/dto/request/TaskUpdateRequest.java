package com.holaho.intern.shared.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskUpdateRequest {
    @Min(0)
    @Max(100)
    private Integer progressPercent;

    @NotBlank(message = "Content is required")
    private String content;
}

