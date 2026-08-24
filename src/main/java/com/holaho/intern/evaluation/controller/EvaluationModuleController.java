package com.holaho.intern.evaluation.controller;

import com.holaho.intern.evaluation.dto.*;
import com.holaho.intern.evaluation.service.EvaluationModuleService;
import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * New evaluation controller implementing the full evaluation lifecycle.
 *
 * Endpoints:
 *   POST   /api/v2/evaluations             — Create evaluation draft (Mentor)
 *   PUT    /api/v2/evaluations/{id}         — Update draft scores (Mentor)
 *   POST   /api/v2/evaluations/{id}/submit  — Submit evaluation (Mentor)
 *   POST   /api/v2/evaluations/{id}/approve — Approve evaluation (HR/Admin)
 *   POST   /api/v2/evaluations/{id}/return  — Return for revision (HR/Admin)
 *   GET    /api/v2/evaluations/pending      — List pending evaluations (Mentor)
 *   GET    /api/v2/evaluations/{id}         — Evaluation detail (All)
 *   GET    /api/v2/evaluations/intern/{id}  — Evaluations for intern (All)
 *   GET    /api/v2/evaluations              — Paginated list (All)
 *   GET    /api/v2/evaluations/me           — My evaluations (Mentor/Intern)
 *
 * Uses /api/v2/ prefix to coexist with the legacy /api/v1/ controller
 * until migration is complete.
 */
@RestController
@RequestMapping("/api/v2/evaluations")
@RequiredArgsConstructor
public class EvaluationModuleController {

    private final EvaluationModuleService evaluationService;

    // ═══════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════

    /**
     * Create a new evaluation draft.
     * Generates evaluation items from the selected template's criteria.
     */
    @PostMapping
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ApiResponse<EvaluationDetailResponse>> createDraft(
            @Valid @RequestBody CreateEvaluationDraftRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        EvaluationDetailResponse response = evaluationService.createDraft(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Evaluation draft created successfully", response));
    }

    /**
     * Update an evaluation draft with criterion scores and comments.
     * Only allowed when status is DRAFT or RETURNED.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ApiResponse<EvaluationDetailResponse>> updateDraft(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEvaluationRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        EvaluationDetailResponse response = evaluationService.updateDraft(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Evaluation updated successfully", response));
    }

    /**
     * Submit an evaluation for HR review.
     * Validates all required criteria are scored and recalculates the final score.
     * After submission, the evaluation becomes read-only for the mentor.
     */
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ApiResponse<EvaluationDetailResponse>> submit(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        EvaluationDetailResponse response = evaluationService.submit(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Evaluation submitted successfully", response));
    }

    /**
     * Approve a submitted evaluation.
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<EvaluationDetailResponse>> approve(@PathVariable Long id) {
        EvaluationDetailResponse response = evaluationService.approve(id);
        return ResponseEntity.ok(ApiResponse.success("Evaluation approved", response));
    }

    /**
     * Return a submitted evaluation to the mentor for revision.
     */
    @PostMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<EvaluationDetailResponse>> returnForRevision(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "");
        EvaluationDetailResponse response = evaluationService.returnForRevision(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Evaluation returned for revision", response));
    }

    // ═══════════════════════════════════════════
    // QUERIES
    // ═══════════════════════════════════════════

    /**
     * List interns pending evaluation for the current mentor.
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ApiResponse<List<PendingEvaluationResponse>>> getPending(
            @AuthenticationPrincipal CustomUserDetails principal) {
        List<PendingEvaluationResponse> pending = evaluationService.getPendingEvaluations(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(pending));
    }

    /**
     * Get a specific evaluation with all details.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<ApiResponse<EvaluationDetailResponse>> getById(@PathVariable Long id) {
        EvaluationDetailResponse response = evaluationService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get all evaluations for a specific intern.
     */
    @GetMapping("/intern/{internId}")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<ApiResponse<List<EvaluationDetailResponse>>> getByIntern(
            @PathVariable Long internId) {
        return ResponseEntity.ok(ApiResponse.success(evaluationService.getByIntern(internId)));
    }

    /**
     * List evaluations with pagination and filters.
     * HR/Admin sees all, Mentor sees own evaluations.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN', 'INTERN')")
    public ResponseEntity<ApiResponse<List<EvaluationDetailResponse>>> getEvaluations(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        // INTERN: view own evaluations
        if (hasRole(principal, "ROLE_INTERN")) {
            List<EvaluationDetailResponse> internEvals = evaluationService.getByIntern(principal.getId());
            return ResponseEntity.ok(ApiResponse.success(internEvals));
        }

        // MENTOR: paginated own evaluations
        if (hasRole(principal, "ROLE_MENTOR")) {
            Page<EvaluationDetailResponse> page = evaluationService.getMentorEvaluations(
                    principal.getId(), period, keyword, pageable);
            return ResponseEntity.ok(ApiResponse.successPage(page));
        }

        // HR/ADMIN: all evaluations
        Page<EvaluationDetailResponse> page = evaluationService.getAllEvaluations(period, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.successPage(page));
    }

    /**
     * Get my evaluations (Mentor: my reviews, Intern: my evaluations).
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('MENTOR', 'INTERN')")
    public ResponseEntity<ApiResponse<?>> getMyEvaluations(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        if (hasRole(principal, "ROLE_MENTOR")) {
            Page<EvaluationDetailResponse> page = evaluationService.getMentorEvaluations(
                    principal.getId(), period, keyword, pageable);
            return ResponseEntity.ok(ApiResponse.successPage(page));
        }
        // INTERN
        List<EvaluationDetailResponse> evals = evaluationService.getByIntern(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(evals));
    }

    // ── Helpers ──

    private boolean hasRole(CustomUserDetails principal, String role) {
        return principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }
}
