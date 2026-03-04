package com.example.backend.controller;

import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.request.ReviewApplicationRequest;
import com.example.backend.dto.request.ApplicationSubmitRequest;
import com.example.backend.dto.response.ApplicationResponse;
import com.example.backend.enums.ApplicationStatus;
import com.example.backend.security.CustomUserDetails;
import com.example.backend.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getMyApplications(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ApplicationResponse> applications = applicationService
                .getMyApplications(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(applications));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<ApplicationResponse>>> search(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<ApplicationResponse> response = applicationService.searchApplications(status, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> submit(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ApplicationSubmitRequest request) {
        ApplicationResponse response = applicationService.submit(request, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Nộp hồ sơ thành công", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> getById(@PathVariable Long id) {
        ApplicationResponse application = applicationService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(application));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> review(
            @PathVariable Long id,
            @Valid @RequestBody ReviewApplicationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ApplicationResponse response = applicationService.reviewApplication(id, request, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Xét duyệt hồ sơ thành công", response));
    }
}
