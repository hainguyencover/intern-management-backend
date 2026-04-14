package com.holaho.intern.service;

import com.holaho.intern.shared.dto.response.CvScreeningResponse;
import java.util.Map;

public interface AiService {
    CvScreeningResponse screenCv(byte[] fileContent, String fileName);

    Map<String, Object> analyzeSentiment(String text);

    Double calculateMatchingScore(Long internId, Long mentorId);
}

