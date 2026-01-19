package com.example.backend.controller;

import com.example.backend.dto.WeeklyReportDto;
import com.example.backend.dto.request.WeeklyReportRequest;
import com.example.backend.security.CustomUserDetails;
import com.example.backend.service.UserService;
import com.example.backend.service.WeeklyReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interns/me/weekly-reports")
@RequiredArgsConstructor
public class InternWeeklyReportController {

    private final WeeklyReportService weeklyReportService;
    private final UserService userService;

    @PreAuthorize("hasRole('INTERN')")
    @PostMapping
    public ResponseEntity<WeeklyReportDto> submit(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody WeeklyReportRequest request) {
        Long internId = userService.getInternProfileIdByUserId(principal.getId());
        WeeklyReportDto report = weeklyReportService.internSubmit(internId, request);
        return ResponseEntity.ok(report);
    }

    @PreAuthorize("hasRole('INTERN')")
    @GetMapping
    public ResponseEntity<Page<WeeklyReportDto>> myReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("weekNumber").descending());
        Long internId = userService.getInternProfileIdByUserId(principal.getId());
        return ResponseEntity.ok(weeklyReportService.internMyReports(internId, pageable));
    }
}
