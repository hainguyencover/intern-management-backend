package com.example.backend.controller;

import com.example.backend.dto.WeeklyReportDto;
import com.example.backend.dto.request.WeeklyReportRequest;
import com.example.backend.dto.response.ApiResponse;
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
@RequestMapping("/api/v1/interns/me/weekly-reports")
@RequiredArgsConstructor
public class InternWeeklyReportController {

    private final WeeklyReportService weeklyReportService;
    private final UserService userService;

    @PreAuthorize("hasRole('INTERN')")
    @PostMapping
    public ResponseEntity<ApiResponse<WeeklyReportDto>> submit(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody WeeklyReportRequest request) {
        Long internId = userService.getInternProfileIdByUserId(principal.getId());
        WeeklyReportDto report = weeklyReportService.internSubmit(internId, request);
        return ResponseEntity.ok(ApiResponse.success("Nộp báo cáo tuần thành công", report));
    }

    @PreAuthorize("hasRole('INTERN')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<WeeklyReportDto>>> myReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("weekNumber").descending());
        Long internId = userService.getInternProfileIdByUserId(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(weeklyReportService.internMyReports(internId, pageable)));
    }
}
