package com.example.backend.controller;

import com.example.backend.dto.InternCountStatDto;
import com.example.backend.service.HrAnalyticsService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/hr/analytics")
public class HrAnalyticsController {

    private final HrAnalyticsService hrAnalyticsService;

    public HrAnalyticsController(HrAnalyticsService hrAnalyticsService) {
        this.hrAnalyticsService = hrAnalyticsService;
    }

    @PreAuthorize("hasRole('HR')")
    @GetMapping("/interns/count-by")
    public List<InternCountStatDto> countBy(@RequestParam(required = false) String groupBy) {
        try {
            return hrAnalyticsService.countInterns(groupBy);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
