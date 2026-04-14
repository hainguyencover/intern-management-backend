package com.holaho.intern.controller;

import com.holaho.intern.shared.dto.request.ReviewApplicationRequest;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.ApplicationResponse;
import com.holaho.intern.shared.enums.ApplicationStatus;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.service.ApplicationService;
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
@RequestMapping("/api/v1/hr/applications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'ADMIN')")
public class HrApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ApplicationResponse>>> list(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "appliedAt"));
        Page<ApplicationResponse> response = applicationService.searchApplications(status, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicationResponse>> detail(@PathVariable Long id) {
        ApplicationResponse response = applicationService.getApplicationById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponse<ApplicationResponse>> review(
            @PathVariable Long id,
            @Valid @RequestBody ReviewApplicationRequest req,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ApplicationResponse response = applicationService.reviewApplication(id, req, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("XÃƒÆ’Ã‚Â©t duyÃƒÂ¡Ã‚Â»Ã¢â‚¬Â¡t hÃƒÂ¡Ã‚Â»Ã¢â‚¬Å“ sÃƒâ€ Ã‚Â¡ thÃƒÆ’Ã‚Â nh cÃƒÆ’Ã‚Â´ng", response));
    }
}

