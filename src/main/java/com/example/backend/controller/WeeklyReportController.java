package com.example.backend.controller;

import com.example.backend.dto.WeeklyReportDto;
import com.example.backend.dto.request.ReviewWeeklyReportRequest;
import com.example.backend.service.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;
    private final com.example.backend.service.UserService userService;

    @PostMapping("/weekly")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<WeeklyReportDto> submitReport(
            @jakarta.validation.Valid @RequestBody com.example.backend.dto.request.WeeklyReportRequest request,
            @AuthenticationPrincipal com.example.backend.security.CustomUserDetails principal) {
        Long internId = userService.getInternProfileIdByUserId(principal.getId());
        return ResponseEntity.ok(weeklyReportService.internSubmit(internId, request));
    }

    @GetMapping("/intern/{internId}")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<List<WeeklyReportDto>> getInternReports(@PathVariable Long internId) {
        return ResponseEntity.ok(weeklyReportService.getByIntern(internId));
    }

    @GetMapping("/final/{internId}")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<com.example.backend.dto.response.FinalReportDto> getFinalReport(@PathVariable Long internId) {
        return ResponseEntity.ok(weeklyReportService.getFinalReport(internId));
    }

    @GetMapping("/dashboard-summary")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<List<com.example.backend.dto.response.FinalReportSummaryDto>> getReportsSummary() {
        return ResponseEntity.ok(weeklyReportService.getReportsSummary());
    }

    @GetMapping("/stats/assessment")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<List<com.example.backend.dto.InternCountStatDto>> getStatsAssessment() {
        return ResponseEntity.ok(weeklyReportService.getStatsByAssessment());
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<WeeklyReportDto> getReportDetail(@PathVariable Long id) {
        return ResponseEntity.ok(weeklyReportService.getReportDetail(id));
    }

    @PutMapping("/{id:\\d+}/feedback")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<WeeklyReportDto> addFeedback(
            @PathVariable Long id,
            @RequestBody ReviewWeeklyReportRequest request,
            @AuthenticationPrincipal com.example.backend.security.CustomUserDetails principal) {
        return ResponseEntity.ok(weeklyReportService.mentorReview(principal.getId(), id, request));
    }

    @PutMapping("/{id:\\d+}/status")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<WeeklyReportDto> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> payload) {
        // Simple status update if needed, otherwise mentorReview covers it
        // For now, if frontend calls this to mark reviewed:
        if ("REVIEWED".equals(payload.get("status"))) {
            // We might need a method that only updates status or reuse review
            // But mentorReview requires a request body with comment.
            // Let's implement a simple status update in service if needed,
            // or assuming frontend uses addFeedback for both.
            // The frontend reportApi.js has markAsReviewed separate.
            // Let's call a new service method or mentorReview with empty comment?
            // Best to add strict method in Service.
            return ResponseEntity.ok(weeklyReportService.updateStatus(id, payload.get("status")));
        }
        return ResponseEntity.badRequest().build();
    }
}
