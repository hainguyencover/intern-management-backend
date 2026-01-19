package com.example.backend.controller;

import com.example.backend.dto.response.AttendanceResponse;
import com.example.backend.entity.Attendance;
import com.example.backend.security.CustomUserDetails;
import com.example.backend.service.AttendanceService;
import com.example.backend.service.UserService;
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
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final UserService userService;

    @PostMapping("/check-in")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<AttendanceResponse> checkIn(@AuthenticationPrincipal CustomUserDetails user) {
        Long internId = userService.getInternProfileIdByUserId(user.getId());
        AttendanceResponse response = attendanceService.checkIn(internId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<AttendanceResponse> checkOut(@AuthenticationPrincipal CustomUserDetails user) {
        Long internId = userService.getInternProfileIdByUserId(user.getId());
        AttendanceResponse response = attendanceService.checkOut(internId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/today")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<AttendanceResponse> getTodayAttendance(@AuthenticationPrincipal CustomUserDetails user) {
        Long internId = userService.getInternProfileIdByUserId(user.getId());
        Attendance attendance = attendanceService.getTodayAttendance(internId);
        return ResponseEntity.ok(attendance != null ? AttendanceResponse.from(attendance) : null);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<Page<AttendanceResponse>> getMyAttendance(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 10, sort = "date") Pageable pageable) {
        Long internId = userService.getInternProfileIdByUserId(user.getId());
        Page<Attendance> page = attendanceService.getAttendanceHistory(
                internId, fromDate, toDate, pageable);
        return ResponseEntity.ok(page.map(AttendanceResponse::from));
    }

    @GetMapping("/report")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<Page<AttendanceResponse>> getAttendanceReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @PageableDefault(size = 10, sort = "date") Pageable pageable) {
        Page<Attendance> page = attendanceService.getAllAttendance(fromDate, toDate, pageable);
        return ResponseEntity.ok(page.map(AttendanceResponse::from));
    }
}
