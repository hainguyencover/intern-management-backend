package com.example.backend.controller;

import com.example.backend.dto.WeeklyReportDto;
import com.example.backend.dto.request.ReviewWeeklyReportRequest;
import com.example.backend.security.CustomUserDetails;
import com.example.backend.service.WeeklyReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mentor/weekly-reports")
@RequiredArgsConstructor
public class MentorWeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    @GetMapping
    public ResponseEntity<Page<WeeklyReportDto>> getReports(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long internId, // Added support for filters.internId
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());
        // For MVP, passing mentorId to filter reports 'owned' or 'visible' to mentor
        return ResponseEntity
                .ok(weeklyReportService.mentorGroupReports(principal.getId(), groupId, internId, status, pageable));
    }

    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<WeeklyReportDto> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(weeklyReportService.getReportDetail(id));
    }

    @PreAuthorize("hasRole('MENTOR')")
    @PostMapping("/{id}/review")
    public ResponseEntity<WeeklyReportDto> review(
            @PathVariable Long id,
            @Valid @RequestBody ReviewWeeklyReportRequest req,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(weeklyReportService.mentorReview(principal.getId(), id, req));
    }
}
