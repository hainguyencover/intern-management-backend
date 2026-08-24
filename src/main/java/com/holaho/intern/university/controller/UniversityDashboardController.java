package com.holaho.intern.university.controller;

import com.holaho.intern.shared.dto.ApiResponse;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.university.dto.UniversityDashboardResponse;
import com.holaho.intern.university.service.UniversityDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/university/dashboard")
@RequiredArgsConstructor
public class UniversityDashboardController {

    private final UniversityDashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasAuthority('UNIVERSITY_DASHBOARD_VIEW') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UniversityDashboardResponse>> getDashboard(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        UniversityDashboardResponse data = dashboardService.getDashboardData(principal);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
