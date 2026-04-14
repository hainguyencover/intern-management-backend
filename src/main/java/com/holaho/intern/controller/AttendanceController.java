package com.holaho.intern.controller;

import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.AttendanceResponse;
import com.holaho.intern.entity.Attendance;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.service.AttendanceService;
import com.holaho.intern.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final UserService userService;

    @PostMapping("/check-in")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkIn(@AuthenticationPrincipal CustomUserDetails user) {
        Long internId = userService.getInternProfileIdByUserId(user.getId());
        AttendanceResponse response = attendanceService.checkIn(internId);
        return ResponseEntity.ok(ApiResponse.success("Check-in successful", response));
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> checkOut(@AuthenticationPrincipal CustomUserDetails user) {
        Long internId = userService.getInternProfileIdByUserId(user.getId());
        AttendanceResponse response = attendanceService.checkOut(internId);
        return ResponseEntity.ok(ApiResponse.success("Check-out successful", response));
    }

    @GetMapping("/today")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> getTodayAttendance(
            @AuthenticationPrincipal CustomUserDetails user) {
        Long internId = userService.getInternProfileIdByUserId(user.getId());
        Attendance attendance = attendanceService.getTodayAttendance(internId);
        AttendanceResponse response = (attendance != null ? AttendanceResponse.from(attendance) : null);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<Page<AttendanceResponse>>> getMyAttendance(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 10, sort = "date") Pageable pageable) {
        Long internId = userService.getInternProfileIdByUserId(user.getId());
        Page<Attendance> page = attendanceService.getAttendanceHistory(
                internId, fromDate, toDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(page.map(AttendanceResponse::from)));
    }

    @GetMapping("/report")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<AttendanceResponse>>> getAttendanceReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 10, sort = "date") Pageable pageable) {
        Page<Attendance> page = attendanceService.getAllAttendance(fromDate, toDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(page.map(AttendanceResponse::from)));
    }
}

