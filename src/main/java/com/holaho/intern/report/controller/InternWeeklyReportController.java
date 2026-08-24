package com.holaho.intern.report.controller;

import com.holaho.intern.report.dto.*;
import com.holaho.intern.report.service.WeeklyReportService;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("reportInternWeeklyReportController")
@RequestMapping("/api/v1/interns/me/weekly-reports")
@RequiredArgsConstructor
public class InternWeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    @PreAuthorize("hasRole('INTERN')")
    @PostMapping
    public ResponseEntity<ApiResponse<WeeklyReportResponse>> createDraft(
            @Valid @RequestBody WeeklyReportCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        WeeklyReportResponse response = weeklyReportService.createDraft(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Tạo dự thảo báo cáo tuần thành công", response));
    }

    @PreAuthorize("hasRole('INTERN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WeeklyReportResponse>> updateDraft(
            @PathVariable Long id,
            @Valid @RequestBody WeeklyReportUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        WeeklyReportResponse response = weeklyReportService.updateDraft(id, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật báo cáo tuần thành công", response));
    }

    @PreAuthorize("hasRole('INTERN')")
    @PostMapping("/{id}/submit")
    public ResponseEntity<ApiResponse<WeeklyReportResponse>> submitReport(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        WeeklyReportResponse response = weeklyReportService.submitReport(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Nộp báo cáo tuần thành công", response));
    }

    @PreAuthorize("hasRole('INTERN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WeeklyReportResponse>> getMyReportById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        WeeklyReportResponse response = weeklyReportService.getMyReportById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('INTERN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<WeeklyReportResponse>>> getMyReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "weekStartDate,desc") String sort,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Sort s = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, s);
        Page<WeeklyReportResponse> response = weeklyReportService.getMyReports(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.successPage(response));
    }

    private Sort parseSort(String sort) {
        try {
            String[] parts = sort.split(",");
            String field = parts[0];
            String dir = parts.length > 1 ? parts[1] : "desc";
            return "asc".equalsIgnoreCase(dir) ? Sort.by(field).ascending() : Sort.by(field).descending();
        } catch (Exception e) {
            return Sort.by("weekStartDate").descending();
        }
    }
}
