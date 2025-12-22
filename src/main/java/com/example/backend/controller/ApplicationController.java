package com.example.backend.controller;

import com.example.backend.dto.request.ApplicationRequest;
import com.example.backend.dto.response.ApplicationResponse;
import com.example.backend.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    public ResponseEntity<ApplicationResponse> submitApplication(@Valid @RequestBody ApplicationRequest req) {
        ApplicationResponse resp = applicationService.submitApplication(req);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/my")
    public ResponseEntity<List<ApplicationResponse>> getMyApplications() {
        List<ApplicationResponse> list = applicationService.getMyApplications();
        return ResponseEntity.ok(list);
    }
}
