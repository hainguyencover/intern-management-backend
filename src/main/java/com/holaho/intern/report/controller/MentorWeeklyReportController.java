package com.holaho.intern.report.controller;

import com.holaho.intern.report.dto.*;
import com.holaho.intern.report.enums.WeeklyReportStatus;
import com.holaho.intern.report.service.WeeklyReportService;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController("reportMentorWeeklyReportController")
@RequestMapping("/api/v1/mentor/weekly-reports")
@RequiredArgsConstructor
public class MentorWeeklyReportController {

    private final WeeklyReportService weeklyReportService;

    @PreAuthorize("hasRole('MENTOR')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<WeeklyReportResponse>>> getMentorReports(
            @RequestParam(required = false) Long internId,
            @RequestParam(required = false) WeeklyReportStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "submittedAt,desc") String sort,
            @AuthenticationPrincipal CustomUserDetails principal) {
        WeeklyReportFilterRequest filter = WeeklyReportFilterRequest.builder()
                .internId(internId)
                .status(status)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();
        Sort s = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, s);
        Page<WeeklyReportResponse> response = weeklyReportService.getMentorReports(principal.getId(), filter, pageable);
        return ResponseEntity.ok(ApiResponse.successPage(response));
    }

    @PreAuthorize("hasRole('MENTOR')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WeeklyReportResponse>> getMentorReportById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        WeeklyReportResponse response = weeklyReportService.getMentorReportById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PreAuthorize("hasRole('MENTOR')")
    @PostMapping("/{id}/feedback")
    public ResponseEntity<ApiResponse<WeeklyReportFeedbackResponse>> addFeedback(
            @PathVariable Long id,
            @Valid @RequestBody WeeklyReportFeedbackRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        WeeklyReportFeedbackResponse response = weeklyReportService.addFeedback(id, principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Gửi phản hồi thành công", response));
    }

    private Sort parseSort(String sort) {
        try {
            String[] parts = sort.split(",");
            String field = parts[0];
            String dir = parts.length > 1 ? parts[1] : "desc";
            return "asc".equalsIgnoreCase(dir) ? Sort.by(field).ascending() : Sort.by(field).descending();
        } catch (Exception e) {
            return Sort.by("submittedAt").descending();
        }
    }
}
