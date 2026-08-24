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
public class AttendanceDetailResponse {
    private Long id;
    private Long internId;
    private String internName;
    private String studentCode;
    private LocalDate date;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private LocalDateTime scheduledStartAt;
    private LocalDateTime scheduledEndAt;
    private Integer totalMinutes;
    private Integer workedMinutes;
    private Integer lateMinutes;
    private Integer earlyLeaveMinutes;
    private String checkInMethod;
    private String checkOutMethod;
    private String status;
    private String note;
    private LocalDateTime createdAt;
}
