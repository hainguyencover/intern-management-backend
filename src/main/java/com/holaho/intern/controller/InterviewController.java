package com.holaho.intern.controller;

import com.holaho.intern.shared.dto.request.InterviewScheduleRequest;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.InterviewResponse;
import com.holaho.intern.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<InterviewResponse>> schedule(
            @Valid @RequestBody InterviewScheduleRequest request) {
        InterviewResponse response = interviewService.scheduleInterview(request);
        return ResponseEntity.ok(ApiResponse.success("Interview scheduled successfully", response));
    }

    @PostMapping("/{id}/feedback")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'MENTOR')")
    public ResponseEntity<ApiResponse<InterviewResponse>> submitFeedback(
            @PathVariable Long id,
            @RequestParam String feedback,
            @RequestParam String status) {
        InterviewResponse response = interviewService.submitFeedback(id, feedback, status);
        return ResponseEntity.ok(ApiResponse.success("Feedback submitted successfully", response));
    }

    @GetMapping("/application/{applicationId}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'MENTOR', 'INTERN')")
    public ResponseEntity<ApiResponse<List<InterviewResponse>>> getByApplication(@PathVariable Long applicationId) {
        List<InterviewResponse> list = interviewService.getInterviewsByApplication(applicationId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }
}
