package com.holaho.intern.controller;

import com.holaho.intern.shared.dto.response.ApiResponse;
import com.holaho.intern.shared.dto.response.CvScreeningResponse;
import com.holaho.intern.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/matching-score")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<ApiResponse<Double>> getMatchingScore(
            @RequestParam Long internId,
            @RequestParam Long mentorId) {
        Double score = aiService.calculateMatchingScore(internId, mentorId);
        return ResponseEntity.ok(ApiResponse.success(score));
    }

    @PostMapping("/analyze-sentiment")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'MENTOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeReportSentiment(
            @RequestBody String text) {
        Map<String, Object> sentiment = aiService.analyzeSentiment(text);
        return ResponseEntity.ok(ApiResponse.success(sentiment));
    }
}

