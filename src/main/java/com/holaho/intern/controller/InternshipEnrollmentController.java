package com.holaho.intern.controller;

import com.holaho.intern.service.InternshipEnrollmentService;
import com.holaho.intern.shared.dto.request.EnrollmentRequest;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.EnrollmentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/programs/{programId}/enrollments")
@RequiredArgsConstructor
public class InternshipEnrollmentController {

    private final InternshipEnrollmentService enrollmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> enrollIntern(
            @PathVariable Long programId,
            @Valid @RequestBody EnrollmentRequest req) {
        EnrollmentResponse res = enrollmentService.enrollIntern(programId, req.getInternId(), req.getGroupId(), req.getJoinedAt());
        return ResponseEntity.ok(ApiResponse.success("Tiếp nhận thực tập sinh vào chương trình thành công", res));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'MENTOR')")
    public ResponseEntity<ApiResponse<java.util.List<EnrollmentResponse>>> getEnrollments(
            @PathVariable Long programId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("joinedAt").descending());
        Page<EnrollmentResponse> res = enrollmentService.getProgramEnrollments(programId, keyword, groupId, pageable);
        return ResponseEntity.ok(ApiResponse.successPage(res));
    }

    @DeleteMapping("/{enrollmentId}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> withdrawEnrollment(
            @PathVariable Long programId,
            @PathVariable Long enrollmentId) {
        enrollmentService.withdrawEnrollment(enrollmentId);
        return ResponseEntity.ok(ApiResponse.success("Đã rút thực tập sinh khỏi chương trình", null));
    }
}
