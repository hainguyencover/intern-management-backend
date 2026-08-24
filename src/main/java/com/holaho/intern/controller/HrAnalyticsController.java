package com.holaho.intern.controller;

import com.holaho.intern.service.HrAnalyticsService;
import com.holaho.intern.shared.dto.InternCountStatDto;
import com.holaho.intern.shared.dto.request.AnalyticsFilterRequest;
import com.holaho.intern.shared.dto.response.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hr/analytics")
@PreAuthorize("hasAnyRole('HR', 'ADMIN')")
public class HrAnalyticsController {

    private final HrAnalyticsService hrAnalyticsService;

    public HrAnalyticsController(HrAnalyticsService hrAnalyticsService) {
        this.hrAnalyticsService = hrAnalyticsService;
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AnalyticsOverviewResponse>> getOverview(
            @Valid AnalyticsFilterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(hrAnalyticsService.getOverview(request)));
    }

    @GetMapping("/interns/by-school")
    public ResponseEntity<ApiResponse<List<SchoolStatisticResponse>>> getBySchool(
            @Valid AnalyticsFilterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(hrAnalyticsService.getBySchool(request)));
    }

    @GetMapping("/interns/by-major")
    public ResponseEntity<ApiResponse<List<MajorStatisticResponse>>> getByMajor(
            @Valid AnalyticsFilterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(hrAnalyticsService.getByMajor(request)));
    }

    @GetMapping("/completion")
    public ResponseEntity<ApiResponse<CompletionStatisticResponse>> getCompletion(
            @Valid AnalyticsFilterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(hrAnalyticsService.getCompletion(request)));
    }

    @GetMapping("/interns/count-by")
    public ResponseEntity<ApiResponse<List<InternCountStatDto>>> countBy(
            @RequestParam(required = false) String groupBy) {
        return ResponseEntity.ok(ApiResponse.success(hrAnalyticsService.countInterns(groupBy)));
    }
}
