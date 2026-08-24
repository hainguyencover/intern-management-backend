package com.holaho.intern.university.controller;

import com.holaho.intern.shared.dto.ApiResponse;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.university.service.UniversityReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/university/reports")
@RequiredArgsConstructor
public class UniversityReportController {

    private final UniversityReportService reportService;

    @GetMapping("/progress")
    @PreAuthorize("hasAuthority('UNIVERSITY_STUDENT_VIEW') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProgressReport(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        Map<String, Object> report = reportService.getProgressReport(principal, from, to);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    @GetMapping("/progress/export")
    @PreAuthorize("hasAuthority('UNIVERSITY_REPORT_EXPORT') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportProgressReport(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(defaultValue = "xlsx") String format
    ) throws IOException {
        byte[] excelBytes = reportService.exportProgressReportToExcel(principal);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"university_progress_report.xlsx\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelBytes);
    }
}
