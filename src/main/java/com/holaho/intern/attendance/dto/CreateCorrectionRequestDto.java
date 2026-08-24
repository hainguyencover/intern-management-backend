package com.holaho.intern.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCorrectionRequestDto {

    @NotNull(message = "Attendance ID is required")
    private Long attendanceId;

    private LocalDateTime requestedCheckIn;
    private LocalDateTime requestedCheckOut;

    @NotBlank(message = "Reason is required")
    private String reason;
}
