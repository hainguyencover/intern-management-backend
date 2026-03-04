package com.example.backend.controller;

import com.example.backend.dto.InternCountStatDto;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.DashboardDtos.InternDashboardResponse;
import com.example.backend.dto.response.DashboardOverviewResponse;
import com.example.backend.security.CustomUserDetails;
import com.example.backend.service.StatisticsService;
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
