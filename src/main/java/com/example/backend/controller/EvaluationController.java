package com.example.backend.controller;

import com.example.backend.dto.request.EvaluationRequest;
import com.example.backend.dto.response.ApiResponse;
import com.example.backend.dto.response.EvaluationResponse;
import com.example.backend.security.CustomUserDetails;
import com.example.backend.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PostMapping
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ApiResponse<EvaluationResponse>> create(
            @Valid @RequestBody EvaluationRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        EvaluationResponse eval = evaluationService.create(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Evaluation created successfully", eval));
    }

    @GetMapping("/intern/{internId}")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<ApiResponse<List<EvaluationResponse>>> getByIntern(
            @PathVariable Long internId) {
        return ResponseEntity.ok(ApiResponse.success(evaluationService.getByIntern(internId)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('MENTOR', 'INTERN')")
    public ResponseEntity<ApiResponse<?>> getMyEvaluations(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        if (principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MENTOR"))) {
            return ResponseEntity.ok(ApiResponse
                    .success(evaluationService.getMentorEvaluations(principal.getId(), period, keyword, pageable)));
        } else if (principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_INTERN"))) {
            return ResponseEntity.ok(ApiResponse.success(evaluationService.getInternEvaluations(principal.getId())));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error(400, "Unauthorized roles"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<EvaluationResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(evaluationService.getById(id)));
    }
}
