package com.example.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AllowanceCreateRequest {
    @NotNull
    private Long internId;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal amount;

    @NotNull
    private LocalDate allowanceMonth;

    @Size(max = 1000)
    private String notes;
}
