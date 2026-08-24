package com.holaho.intern.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorrectionResponse {
    private Long id;
    private Long attendanceId;
    private LocalDate attendanceDate;
    private Long internId;
    private String internName;
    private String studentCode;

    private LocalDateTime currentCheckIn;
    private LocalDateTime currentCheckOut;
    private LocalDateTime requestedCheckIn;
    private LocalDateTime requestedCheckOut;

    private String reason;
    private String status;

    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private String reviewComment;
    private LocalDateTime createdAt;
}
