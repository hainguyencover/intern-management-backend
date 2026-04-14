package com.holaho.intern.controller;

import com.holaho.intern.shared.dto.ProgramCompletionStatDto;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.service.HrProgramAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hr/analytics")
public class HrProgramAnalyticsController {

    private final HrProgramAnalyticsService hrProgramAnalyticsService;

    public HrProgramAnalyticsController(HrProgramAnalyticsService hrProgramAnalyticsService) {
        this.hrProgramAnalyticsService = hrProgramAnalyticsService;
    }

    @PreAuthorize("hasRole('HR')")
    @GetMapping("/programs/completion-rate")
    public ResponseEntity<ApiResponse<List<ProgramCompletionStatDto>>> completionRate(
            @RequestParam(required = false) String period, // default FINAL
            @RequestParam(required = false) Long departmentId // optional
    ) {
        return ResponseEntity
                .ok(ApiResponse.success(hrProgramAnalyticsService.completionRateByProgram(period, departmentId)));
    }
}

