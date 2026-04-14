package com.holaho.intern.controller;

import com.holaho.intern.shared.dto.response.DashboardDtos;


import com.holaho.intern.shared.dto.InternCountStatDto;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.DashboardDtos.InternDashboardResponse;
import com.holaho.intern.shared.dto.response.DashboardOverviewResponse;
import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<DashboardOverviewResponse>> getOverview() {
        return ResponseEntity.ok(ApiResponse.success(statisticsService.getOverview()));
    }

    @GetMapping("/hr")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<DashboardOverviewResponse>> getHrDashboard() {
        return ResponseEntity.ok(ApiResponse.success(statisticsService.getOverview()));
    }

    @GetMapping("/university-stats")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<InternCountStatDto>>> getUniversityStats() {
        return ResponseEntity.ok(ApiResponse.success(statisticsService.getUniversityStats()));
    }

    @GetMapping("/intern")
    @PreAuthorize("hasRole('INTERN')")
    public ResponseEntity<ApiResponse<InternDashboardResponse>> getInternDashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Authentication required"));
        }
        return ResponseEntity.ok(ApiResponse.success(statisticsService.getInternDashboard(userDetails.getId())));
    }
}

