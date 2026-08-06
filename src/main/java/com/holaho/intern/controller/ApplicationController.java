package com.holaho.intern.controller;

import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.request.ReviewApplicationRequest;
import com.holaho.intern.shared.dto.request.ApplicationSubmitRequest;
import com.holaho.intern.shared.dto.response.ApplicationResponse;
import com.holaho.intern.shared.enums.ApplicationStatus;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.service.ApplicationService;
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
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> search(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<ApplicationResponse> response = applicationService.searchApplications(status, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.successPage(response));
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

    @PostMapping("/{id}/ai-rescan")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> rescanAi(@PathVariable Long id) {
        applicationService.triggerAiScreening(id);
        return ResponseEntity.ok(ApiResponse.success("Đã kích hoạt lại AI screening"));
    }
}
