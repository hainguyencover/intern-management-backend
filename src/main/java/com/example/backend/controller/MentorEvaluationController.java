package com.example.backend.controller;

import com.example.backend.dto.request.UpsertEvaluationRequest;
import com.example.backend.dto.response.EvaluationResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.service.EvaluationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mentor/evaluations")
@RequiredArgsConstructor
public class MentorEvaluationController {

    private final EvaluationService evaluationService;

    @PreAuthorize("hasRole('MENTOR')")
    @PostMapping
    public ResponseEntity<EvaluationResponse> upsert(@Valid @RequestBody UpsertEvaluationRequest req) {
        return ResponseEntity.ok(evaluationService.upsert(req));
    }

    @PreAuthorize("hasRole('MENTOR')")
    @GetMapping("/me")
    public ResponseEntity<PageResponse<EvaluationResponse>> myEvaluations(Pageable pageable) {
        return ResponseEntity.ok(evaluationService.myEvaluations(pageable));
    }

    @PreAuthorize("hasRole('MENTOR')")
    @GetMapping("/intern/{internId}")
    public ResponseEntity<PageResponse<EvaluationResponse>> internEvaluations(
            @PathVariable Long internId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(evaluationService.evaluationsOfIntern(internId, pageable));
    }
}
