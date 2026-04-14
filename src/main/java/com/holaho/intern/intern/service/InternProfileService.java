package com.holaho.intern.intern.service;

import com.holaho.intern.shared.dto.InternCountStatDto;


import com.holaho.intern.shared.dto.InternSearchCriteria;
import com.holaho.intern.shared.dto.request.InternProfileRequest;
import com.holaho.intern.shared.dto.response.InternProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InternProfileService {
    InternProfileResponse createIntern(InternProfileRequest request);
    InternProfileResponse updateIntern(Long id, InternProfileRequest request);
    void deleteIntern(Long id);
    InternProfileResponse getInternProfileById(Long id);
    InternProfileResponse getInternProfileByUserId(Long userId);
    void delete(Long id);
    InternProfileResponse getInternProfile(Long id);
    InternProfileResponse getMyProfile(Long userId);
    Page<InternProfileResponse> searchInterns(InternSearchCriteria criteria, Pageable pageable);
    List<String> getAllUniversities();
    List<String> getAllMajors();
    void deleteInternProfile(Long id);
    InternProfileResponse updateMyProfile(InternProfileRequest request, String email);
    void assignMentorByUserId(Long internId, Long mentorUserId);
    List<com.holaho.intern.shared.dto.InternCountStatDto> getInternStatsByUniversity();
    List<com.holaho.intern.shared.dto.InternCountStatDto> getInternStatsByMajor();
}

