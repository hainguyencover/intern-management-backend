package com.holaho.intern.shared.dto.response;

import com.holaho.intern.entity.Attendance;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AttendanceResponse {
    private Long id;
    private Long internId;
    private String internName;
    private LocalDate date;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private Integer totalMinutes;
    private String note;
    private String status;

    public static AttendanceResponse from(Attendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .internId(attendance.getIntern().getId())
                .internName(attendance.getIntern().getUser().getFullName())
                .date(attendance.getDate())
                .checkIn(attendance.getCheckIn())
                .checkOut(attendance.getCheckOut())
                .totalMinutes(attendance.getTotalMinutes())
                .note(attendance.getNote())
                .status(attendance.getStatus())
                .build();
    }
}

