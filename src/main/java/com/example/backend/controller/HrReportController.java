package com.example.backend.controller;

import com.example.backend.dto.response.AttendanceDailyItem;
import com.example.backend.dto.response.AttendanceSummaryItem;
import com.example.backend.dto.response.FinalEvaluationReportResponse;
import com.example.backend.service.AttendanceReportService;
import com.example.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hr/reports")
public class HrReportController {

    private final ReportService reportService;
    private final AttendanceReportService attendanceReportService;

    @GetMapping("/evaluations")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public FinalEvaluationReportResponse getReport(
            @RequestParam String period
    ) {
        return reportService.generateFinalEvaluationReport(period);
    }

    @GetMapping("/evaluations/group/{groupId}")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public FinalEvaluationReportResponse getReportByGroup(
            @PathVariable Long groupId,
            @RequestParam String period
    ) {
        return reportService.generateFinalEvaluationReportByGroup(groupId, period);
    }

    @GetMapping("/evaluations/export")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam String period,
            @RequestParam(defaultValue = "xlsx") String type
    ) {
        FinalEvaluationReportResponse report = reportService.generateFinalEvaluationReport(period);
        byte[] file = reportService.exportFinalEvaluationReportExcel(report);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=final-report-" + period + ".xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    @GetMapping("/evaluations/group/{groupId}/export")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportExcelByGroup(
            @PathVariable Long groupId,
            @RequestParam String period
    ) {
        FinalEvaluationReportResponse report = reportService.generateFinalEvaluationReportByGroup(groupId, period);
        byte[] file = reportService.exportFinalEvaluationReportExcel(report);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=final-report-group-" + groupId + "-" + period + ".xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    @GetMapping("/attendance/summary")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public List<AttendanceSummaryItem> attendanceSummary(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to
    ) {
        return attendanceReportService.summary(from, to);
    }

    @GetMapping("/attendance/daily")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public List<AttendanceDailyItem> attendanceDaily(
            @RequestParam Long internId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to
    ) {
        return attendanceReportService.daily(internId, from, to);
    }
}
