package com.example.backend.controller;

import com.example.backend.dto.InternCountStatDto;
import com.example.backend.dto.response.DashboardOverviewResponse;
import com.example.backend.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<DashboardOverviewResponse> getOverview() {
        return ResponseEntity.ok(statisticsService.getOverview());
    }

    @GetMapping("/hr")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<DashboardOverviewResponse> getHrDashboard() {
        return ResponseEntity.ok(statisticsService.getOverview());
    }

    @GetMapping("/university-stats")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<List<InternCountStatDto>> getUniversityStats() {
        return ResponseEntity.ok(statisticsService.getUniversityStats());
    }
}
