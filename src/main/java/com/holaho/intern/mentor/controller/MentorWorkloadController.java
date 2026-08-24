package com.holaho.intern.mentor.controller;

import com.holaho.intern.mentor.dto.MentorActiveInternResponse;
import com.holaho.intern.mentor.dto.MentorWorkloadResponse;
import com.holaho.intern.mentor.dto.MentorWorkloadSummaryResponse;
import com.holaho.intern.mentor.service.MentorWorkloadService;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.enums.MentorWorkloadStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hr/mentors/workloads")
@RequiredArgsConstructor
public class MentorWorkloadController {

    private final MentorWorkloadService mentorWorkloadService;

    @GetMapping
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<Page<MentorWorkloadResponse>>> getWorkloads(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) MentorWorkloadStatus workloadStatus,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MentorWorkloadResponse> result = mentorWorkloadService.getWorkloads(
                keyword,
                departmentId,
                workloadStatus,
                pageable
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<MentorWorkloadSummaryResponse>> getWorkloadSummary() {
        MentorWorkloadSummaryResponse summary = mentorWorkloadService.getWorkloadSummary();
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/{mentorId}/interns")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<ApiResponse<List<MentorActiveInternResponse>>> getMentorActiveInterns(
            @PathVariable Long mentorId
    ) {
        List<MentorActiveInternResponse> interns = mentorWorkloadService.getMentorActiveInterns(mentorId);
        return ResponseEntity.ok(ApiResponse.success(interns));
    }
}
