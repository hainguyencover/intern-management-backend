package com.holaho.intern.controller;

import com.holaho.intern.shared.dto.WeeklyReportDto;
import com.holaho.intern.shared.dto.request.ReviewWeeklyReportRequest;
import com.holaho.intern.shared.dto.request.WeeklyReportRequest;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.FinalReportDto;
import com.holaho.intern.shared.dto.response.FinalReportSummaryDto;
import com.holaho.intern.shared.dto.InternCountStatDto;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.service.WeeklyReportService;
import com.holaho.intern.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;
    private final UserService userService;

    @PostMapping("/weekly")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<WeeklyReportDto>> submitReport(
            @Valid @RequestBody WeeklyReportRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long internId = userService.getInternProfileIdByUserId(principal.getId());
        WeeklyReportDto response = weeklyReportService.internSubmit(internId, request);
        return ResponseEntity.ok(ApiResponse.success("Report submitted successfully", response));
    }

    @GetMapping("/intern/{internId}")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<WeeklyReportDto>>> getInternReports(@PathVariable Long internId) {
        return ResponseEntity.ok(ApiResponse.success(weeklyReportService.getByIntern(internId)));
    }

    @GetMapping("/final/{internId}")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<FinalReportDto>> getFinalReport(@PathVariable Long internId) {
        return ResponseEntity.ok(ApiResponse.success(weeklyReportService.getFinalReport(internId)));
    }

    @GetMapping("/dashboard-summary")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<FinalReportSummaryDto>>> getReportsSummary() {
        return ResponseEntity.ok(ApiResponse.success(weeklyReportService.getReportsSummary()));
    }

    @GetMapping("/stats/assessment")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<InternCountStatDto>>> getStatsAssessment() {
        return ResponseEntity.ok(ApiResponse.success(weeklyReportService.getStatsByAssessment()));
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<ApiResponse<WeeklyReportDto>> getReportDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(weeklyReportService.getReportDetail(id)));
    }

    @PutMapping("/{id:\\d+}/feedback")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<WeeklyReportDto>> addFeedback(
            @PathVariable Long id,
            @RequestBody ReviewWeeklyReportRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        WeeklyReportDto response = weeklyReportService.mentorReview(principal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Feedback added successfully", response));
    }

    @PutMapping("/{id:\\d+}/status")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<WeeklyReportDto>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        if ("REVIEWED".equals(payload.get("status"))) {
            WeeklyReportDto response = weeklyReportService.updateStatus(id, payload.get("status"));
            return ResponseEntity.ok(ApiResponse.success("Report marked as reviewed", response));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error(400, "Invalid status update"));
    }
}

