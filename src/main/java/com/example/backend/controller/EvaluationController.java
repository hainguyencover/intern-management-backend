package com.example.backend.controller;

import com.example.backend.dto.request.EvaluationRequest;
import com.example.backend.security.CustomUserDetails;
import com.example.backend.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PostMapping
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<com.example.backend.dto.response.EvaluationResponse> create(
            @Valid @RequestBody EvaluationRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        com.example.backend.dto.response.EvaluationResponse eval = evaluationService.create(request, principal.getId());
        return ResponseEntity.ok(eval);
    }

    @GetMapping("/intern/{internId}")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<List<com.example.backend.dto.response.EvaluationResponse>> getByIntern(
            @PathVariable Long internId) {
        return ResponseEntity.ok(evaluationService.getByIntern(internId));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('MENTOR', 'INTERN')")
    public ResponseEntity<List<com.example.backend.dto.response.EvaluationResponse>> getMyEvaluations(
            @AuthenticationPrincipal CustomUserDetails principal) {
        if (principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MENTOR"))) {
            return ResponseEntity.ok(evaluationService.getMentorEvaluations(principal.getId()));
        } else if (principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_INTERN"))) {
            return ResponseEntity.ok(evaluationService.getInternEvaluations(principal.getId()));
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<com.example.backend.dto.response.EvaluationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(evaluationService.getById(id));
    }
}
