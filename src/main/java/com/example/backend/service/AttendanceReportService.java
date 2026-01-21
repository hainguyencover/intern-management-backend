package com.example.backend.service;

import com.example.backend.dto.response.AttendanceDailyItem;
import com.example.backend.dto.response.AttendanceSummaryItem;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceReportService {
    List<AttendanceSummaryItem> summary(LocalDate from, LocalDate to);
    List<AttendanceDailyItem> daily(Long internId, LocalDate from, LocalDate to);
}
