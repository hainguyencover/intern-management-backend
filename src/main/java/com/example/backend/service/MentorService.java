package com.example.backend.service;

import com.example.backend.dto.request.MentorCreateRequest;
import com.example.backend.dto.response.MentorResponse;

public interface MentorService {
    MentorResponse createMentor(MentorCreateRequest req);
}
