package com.example.backend.service;

import com.example.backend.dto.request.MentorCreateRequest;
import com.example.backend.dto.response.MentorResponseDto;
import org.springframework.data.domain.Page;

public interface MentorService {

    Page<MentorResponseDto> getMentors(Integer page, Integer size);

    MentorResponseDto createMentor(MentorCreateRequest request);

    MentorResponseDto getMentorById(Long id);
}
