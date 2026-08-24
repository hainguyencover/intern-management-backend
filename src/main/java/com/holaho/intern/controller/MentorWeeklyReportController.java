package com.holaho.intern.controller;

import com.holaho.intern.shared.dto.WeeklyReportDto;
import com.holaho.intern.shared.dto.request.ReviewWeeklyReportRequest;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.service.WeeklyReportService;
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

//@RestController
//@RequestMapping("/api/v1/mentor/weekly-reports")
@RequiredArgsConstructor
@Deprecated
class MentorWeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<WeeklyReportDto>>> getReports(
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long internId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                weeklyReportService.mentorGroupReports(principal.getId(), groupId, internId, status, pageable)));
    }

    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WeeklyReportDto>> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(weeklyReportService.getReportDetail(id)));
    }

    @PreAuthorize("hasRole('MENTOR')")
    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponse<WeeklyReportDto>> review(
            @PathVariable Long id,
            @Valid @RequestBody ReviewWeeklyReportRequest req,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Đánh giá báo cáo tuần thành công",
                weeklyReportService.mentorReview(principal.getId(), id, req)));
    }
}
