package com.example.backend.service;

import com.example.backend.dto.request.CreateMentorRequest;
import com.example.backend.dto.response.InternProfileResponse;
import com.example.backend.dto.response.MentorDashboardStats;
import com.example.backend.dto.response.MentorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MentorService {
    MentorResponse createMentor(CreateMentorRequest request);
    List<MentorResponse> getAllMentors();
    Page<MentorResponse> getMentors(Pageable pageable);
    MentorResponse getMentorById(Long id);
    MentorResponse getMentorByUserId(Long userId);
    MentorDashboardStats getDashboardStats(String email);
    Page<InternProfileResponse> getAssignedInterns(String email, String keyword, String status, Pageable pageable);
    InternProfileResponse getInternDetail(Long internId);
    MentorResponse updateMentor(Long id, CreateMentorRequest request);
}
