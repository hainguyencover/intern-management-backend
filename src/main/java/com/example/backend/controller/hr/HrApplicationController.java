package com.example.backend.controller.hr;

import com.example.backend.dto.ApplicationDetailDto;
import com.example.backend.dto.ApplicationSummaryDto;
import com.example.backend.dto.request.ReviewApplicationRequest;
import com.example.backend.enums.ApplicationStatus;
import com.example.backend.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hr/applications")
@RequiredArgsConstructor
@PreAuthorize("hasRole('HR')")
public class HrApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    public Page<ApplicationSummaryDto> list(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "appliedAt"));
        return applicationService.listForHr(status, pageable);
    }

    @GetMapping("/{id}")
    public ApplicationDetailDto detail(@PathVariable Long id) {
        return applicationService.getDetail(id);
    }

    @PostMapping("/{id}/review")
    public ApplicationDetailDto review(@PathVariable Long id, @Valid @RequestBody ReviewApplicationRequest req) {
        return applicationService.review(id, req);
    }
}
