package com.holaho.intern.controller;

import com.holaho.intern.shared.dto.InternCountStatDto;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.service.HrAnalyticsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hr/analytics")
public class HrAnalyticsController {

    private final HrAnalyticsService hrAnalyticsService;

    public HrAnalyticsController(HrAnalyticsService hrAnalyticsService) {
        this.hrAnalyticsService = hrAnalyticsService;
    }

    @PreAuthorize("hasRole('HR')")
    @GetMapping("/interns/count-by")
    public ResponseEntity<ApiResponse<List<InternCountStatDto>>> countBy(
            @RequestParam(required = false) String groupBy) {
        try {
            return ResponseEntity.ok(ApiResponse.success(hrAnalyticsService.countInterns(groupBy)));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}

