package com.example.backend.controller.hr;

import com.example.backend.dto.request.ReviewApplicationRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.ApplicationResponse;
import com.example.backend.enums.ApplicationStatus;
import com.example.backend.security.CustomUserDetails;
import com.example.backend.service.ApplicationService;
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
@RequestMapping("/api/hr/applications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'ADMIN')")
public class HrApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    public ResponseEntity<Page<ApplicationResponse>> list(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "appliedAt"));
        Page<ApplicationResponse> response = applicationService.searchApplications(status, keyword, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> detail(@PathVariable Long id) {
        ApplicationResponse response = applicationService.getApplicationById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponse<ApplicationResponse>> review(
            @PathVariable Long id,
            @Valid @RequestBody ReviewApplicationRequest req,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ApplicationResponse response = applicationService.reviewApplication(id, req, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Xét duyệt hồ sơ thành công", response));
    }
}
