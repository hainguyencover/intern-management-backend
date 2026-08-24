package com.holaho.intern.evaluation.controller;

import com.holaho.intern.evaluation.dto.EvaluationTemplateResponse;
import com.holaho.intern.evaluation.service.EvaluationTemplateService;
import com.holaho.intern.shared.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/evaluation-templates")
@RequiredArgsConstructor
public class EvaluationTemplateController {

    private final EvaluationTemplateService templateService;

    /**
     * List all active evaluation templates for the current tenant.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<EvaluationTemplateResponse>>> listTemplates() {
        List<EvaluationTemplateResponse> templates = templateService.listActiveTemplates();
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    /**
     * Get a specific template with its criteria.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MENTOR', 'HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<EvaluationTemplateResponse>> getTemplate(@PathVariable Long id) {
        EvaluationTemplateResponse template = templateService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(template));
    }
}
