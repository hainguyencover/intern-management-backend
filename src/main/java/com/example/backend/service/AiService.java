package com.example.backend.service;

import com.example.backend.dto.response.CvScreeningResponse;
import java.util.Map;

public interface AiService {
    CvScreeningResponse screenCv(byte[] fileContent, String fileName);

    Map<String, Object> analyzeSentiment(String text);

    Double calculateMatchingScore(Long internId, Long mentorId);
}
