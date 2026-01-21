package com.example.backend.dto.response;

import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class AttendanceSummaryItem {
    private Long internId;
    private int presentDays;
    private int leaveDays;
    private int absentDays;
}
