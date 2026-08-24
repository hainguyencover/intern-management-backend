package com.holaho.intern.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceSummaryResponse {
    private long totalInterns;
    private long totalWorkingDays;
    private long presentDays;
    private long lateDays;
    private long earlyLeaveDays;
    private long absentDays;
    private long leaveDays;
    private double attendanceRate;
}
