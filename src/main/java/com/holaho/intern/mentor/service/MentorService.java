package com.holaho.intern.mentor.service;

import com.holaho.intern.mentor.dto.CreateMentorRequest;
import com.holaho.intern.mentor.dto.MentorStatusUpdateRequest;
import com.holaho.intern.mentor.dto.UpdateMentorRequest;
import com.holaho.intern.shared.dto.response.InternProfileResponse;
import com.holaho.intern.shared.dto.response.MentorDashboardStats;
import com.holaho.intern.shared.dto.response.MentorResponse;
import com.holaho.intern.shared.enums.MentorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MentorService {
    MentorResponse createMentor(CreateMentorRequest request);
    MentorResponse updateMentor(Long id, UpdateMentorRequest request);
    MentorResponse updateMentorStatus(Long id, MentorStatusUpdateRequest request);

    List<MentorResponse> getAllMentors();
    Page<MentorResponse> getMentors(Pageable pageable);
    Page<MentorResponse> searchMentors(String search, MentorStatus status, Long departmentId, Pageable pageable);

    MentorResponse getMentorById(Long id);
    MentorResponse getMentorByUserId(Long userId);
    MentorDashboardStats getDashboardStats(String email);

    Page<InternProfileResponse> getAssignedInterns(String email, String keyword, String status, Pageable pageable);
    InternProfileResponse getInternDetail(Long internId);

    void deleteMentor(Long id);
}
