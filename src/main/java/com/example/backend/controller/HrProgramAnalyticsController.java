package com.example.backend.controller;

import com.example.backend.dto.ProgramCompletionStatDto;
import com.example.backend.service.HrProgramAnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hr/analytics")
public class HrProgramAnalyticsController {

    private final HrProgramAnalyticsService hrProgramAnalyticsService;

    public HrProgramAnalyticsController(HrProgramAnalyticsService hrProgramAnalyticsService) {
        this.hrProgramAnalyticsService = hrProgramAnalyticsService;
    }

    @PreAuthorize("hasRole('HR')")
    @GetMapping("/programs/completion-rate")
    public List<ProgramCompletionStatDto> completionRate(
            @RequestParam(required = false) String period,          // default FINAL
            @RequestParam(required = false) Long departmentId        // optional
    ) {
        return hrProgramAnalyticsService.completionRateByProgram(period, departmentId);
    }
}
