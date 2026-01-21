package com.example.backend.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AttendanceDailyItem {
    private LocalDate date;
    private String status; // PRESENT / LEAVE / ABSENT
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private String leaveType;
    private String leaveReason;
}
