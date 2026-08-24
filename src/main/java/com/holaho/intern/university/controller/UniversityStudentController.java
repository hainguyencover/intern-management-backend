package com.holaho.intern.university.controller;

import com.holaho.intern.shared.dto.ApiResponse;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.university.dto.UniversityStudentFilter;
import com.holaho.intern.university.dto.UniversityStudentResponse;
import com.holaho.intern.university.service.UniversityStudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/university/students")
@RequiredArgsConstructor
public class UniversityStudentController {

    private final UniversityStudentService studentService;

    @GetMapping
    @PreAuthorize("hasAuthority('UNIVERSITY_STUDENT_VIEW') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UniversityStudentResponse>>> getStudents(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) Double progressMin,
            @RequestParam(required = false) Double progressMax,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UniversityStudentFilter filter = UniversityStudentFilter.builder()
                .keyword(keyword)
                .status(status)
                .major(major)
                .progressMin(progressMin)
                .progressMax(progressMax)
                .build();

        Page<UniversityStudentResponse> page = studentService.findStudents(principal, filter, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/{studentId}")
    @PreAuthorize("hasAuthority('UNIVERSITY_STUDENT_VIEW') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UniversityStudentResponse>> getStudentDetail(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long studentId
    ) {
        UniversityStudentResponse student = studentService.getStudentDetail(principal, studentId);
        return ResponseEntity.ok(ApiResponse.success(student));
    }
}
