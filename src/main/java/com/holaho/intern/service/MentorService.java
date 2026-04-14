package com.holaho.intern.service;

import com.holaho.intern.shared.dto.request.CreateMentorRequest;
import com.holaho.intern.shared.dto.response.InternProfileResponse;
import com.holaho.intern.shared.dto.response.MentorDashboardStats;
import com.holaho.intern.shared.dto.response.MentorResponse;
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
    void deleteMentor(Long id);
}
