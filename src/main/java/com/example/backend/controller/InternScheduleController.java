package com.example.backend.controller;

import com.example.backend.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interns/me")
@RequiredArgsConstructor
public class InternScheduleController {

    private final ScheduleService scheduleService;

    @PreAuthorize("hasRole('INTERN')")
    @GetMapping("/schedule")
    public ResponseEntity<java.util.List<com.example.backend.dto.response.ScheduleEventResponse>> mySchedule(
            Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(scheduleService.getMySchedule(email));
    }
}
