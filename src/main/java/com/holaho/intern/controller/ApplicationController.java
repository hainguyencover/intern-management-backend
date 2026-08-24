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

    /**
     * US-048: Start Review Process
     * POST /api/v1/applications/{id}/start-review
     */
    @PostMapping("/{id}/start-review")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> startReview(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ApplicationResponse response = applicationService.startReview(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Bắt đầu xét duyệt hồ sơ thành công", response));
    }

    /**
     * US-049: Automated Eligibility Screening Engine
     * GET /api/v1/applications/{id}/eligibility
     */
    @GetMapping("/{id}/eligibility")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<com.holaho.intern.shared.dto.response.EligibilityCheckResponse>> checkEligibility(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.checkEligibility(id)));
    }

    /**
     * US-050: Request Revision by HR
     * POST /api/v1/applications/{id}/request-revision
     */
    @PostMapping("/{id}/request-revision")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> requestRevision(
            @PathVariable Long id,
            @RequestParam String comment,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ApplicationResponse response = applicationService.requestRevision(id, userDetails.getId(), comment);
        return ResponseEntity.ok(ApiResponse.success("Đã yêu cầu ứng viên bổ sung hồ sơ", response));
    }

    /**
     * US-050: Candidate Resubmit Profile
     * POST /api/v1/applications/{id}/resubmit
     */
    @PostMapping("/{id}/resubmit")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> resubmit(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ApplicationResponse response = applicationService.resubmit(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Nộp lại hồ sơ ứng tuyển thành công", response));
    }

    /**
     * US-051: Status Audit History
     * GET /api/v1/applications/{id}/history
     */
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<ApiResponse<List<com.holaho.intern.shared.dto.response.StatusHistoryResponse>>> getStatusHistory(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.getStatusHistory(id)));
    }

    /**
     * US-052: Review Queue & Overdue SLA Statistics
     * GET /api/v1/applications/queue
     */
    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<com.holaho.intern.shared.dto.response.ReviewQueueStatsDto>> getReviewQueueStats() {
        return ResponseEntity.ok(ApiResponse.success(applicationService.getReviewQueueStats()));
    }

    /**
     * US-053: Candidate Result View
     * GET /api/v1/applications/me/latest-result
     */
    @GetMapping("/me/latest-result")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<com.holaho.intern.shared.dto.response.CandidateResultResponse>> getLatestCandidateResult(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.getLatestCandidateResult(userDetails.getId())));
    }
}
