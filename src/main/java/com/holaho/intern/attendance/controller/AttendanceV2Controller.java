package com.holaho.intern.attendance.controller;

import com.holaho.intern.attendance.dto.AttendanceDetailResponse;
import com.holaho.intern.attendance.dto.AttendanceSummaryResponse;
import com.holaho.intern.attendance.dto.CheckInRequestDto;
import com.holaho.intern.attendance.dto.CheckOutRequestDto;
import com.holaho.intern.attendance.service.AttendanceCoreService;
import com.holaho.intern.attendance.service.AttendanceReportService;
import com.holaho.intern.repository.AttendanceRepository;
import com.holaho.intern.shared.config.TenantContext;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Attendance Management Controller v2 (US-A01 to US-A10).
 */
@RestController
@RequestMapping("/api/v2/attendance")
@RequiredArgsConstructor
public class AttendanceV2Controller {

    private final AttendanceCoreService coreService;
    private final AttendanceReportService reportService;
    private final AttendanceRepository attendanceRepository;
    private final UserService userService;

    @PostMapping("/check-in")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<AttendanceDetailResponse>> checkIn(
            @RequestBody(required = false) CheckInRequestDto req,
            @AuthenticationPrincipal CustomUserDetails user) {
        Long internId = userService.getInternProfileIdByUserId(user.getId());
        AttendanceDetailResponse response = coreService.checkIn(internId, req);
        return ResponseEntity.ok(ApiResponse.success("Check-in thành công", response));
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<AttendanceDetailResponse>> checkOut(
            @RequestBody(required = false) CheckOutRequestDto req,
            @AuthenticationPrincipal CustomUserDetails user) {
        Long internId = userService.getInternProfileIdByUserId(user.getId());
        AttendanceDetailResponse response = coreService.checkOut(internId, req);
        return ResponseEntity.ok(ApiResponse.success("Check-out thành công", response));
    }

    @GetMapping("/today")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<AttendanceDetailResponse>> getToday(
            @AuthenticationPrincipal CustomUserDetails user) {
        Long internId = userService.getInternProfileIdByUserId(user.getId());
        AttendanceDetailResponse response = coreService.getTodayAttendance(internId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<List<AttendanceDetailResponse>>> getMyHistory(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Pageable pageable) {
        Long internId = userService.getInternProfileIdByUserId(user.getId());
        Page<AttendanceDetailResponse> page = coreService.getInternHistory(internId, fromDate, toDate, pageable);
        return ResponseEntity.ok(ApiResponse.successPage(page));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'MENTOR')")
    public ResponseEntity<ApiResponse<List<AttendanceDetailResponse>>> getAttendanceList(
            @RequestParam(required = false) Long internId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        Long tenantId = TenantContext.getCurrentTenantId() != null ? TenantContext.getCurrentTenantId() : 1L;
        Page<AttendanceDetailResponse> page = attendanceRepository
                .findWithFilters(tenantId, internId, status, fromDate, toDate, keyword, pageable)
                .map(coreService::mapToDetailResponse);
        return ResponseEntity.ok(ApiResponse.successPage(page));
    }

    @GetMapping("/reports/summary")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<AttendanceSummaryResponse>> getSummaryReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        AttendanceSummaryResponse summary = reportService.getSummary(fromDate, toDate);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
