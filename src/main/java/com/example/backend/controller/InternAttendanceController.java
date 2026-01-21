package com.example.backend.controller;

import com.example.backend.dto.request.CheckOutRequest;
import com.example.backend.dto.response.AttendanceResponse;
import com.example.backend.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/intern/attendance")
public class InternAttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    @PreAuthorize("hasRole('INTERN')")
    public AttendanceResponse checkIn() {
        return attendanceService.checkIn();
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasRole('INTERN')")
    public AttendanceResponse checkOut(@RequestBody(required = false) CheckOutRequest req) {
        return attendanceService.checkOut(req);
    }

    @GetMapping("/today")
    @PreAuthorize("hasRole('INTERN')")
    public AttendanceResponse today() {
        return attendanceService.today();
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('INTERN')")
    public List<AttendanceResponse> history(
            @RequestParam int month,
            @RequestParam int year
    ) {
        return attendanceService.history(month, year);
    }
}
