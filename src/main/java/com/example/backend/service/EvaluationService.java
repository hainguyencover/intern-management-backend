package com.example.backend.service;

import com.example.backend.dto.request.UpsertEvaluationRequest;
import com.example.backend.dto.response.EvaluationResponse;
import com.example.backend.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface EvaluationService {
    EvaluationResponse upsert(UpsertEvaluationRequest req);

    PageResponse<EvaluationResponse> myEvaluations(Pageable pageable);

    PageResponse<EvaluationResponse> evaluationsOfIntern(Long internId, Pageable pageable);
}
