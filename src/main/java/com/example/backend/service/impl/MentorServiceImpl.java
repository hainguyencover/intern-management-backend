package com.example.backend.service.impl;

import com.example.backend.dto.DepartmentSummaryDto;
import com.example.backend.dto.UserSummaryDto;
import com.example.backend.dto.request.MentorCreateRequest;
import com.example.backend.dto.response.MentorResponseDto;
import com.example.backend.dto.response.MentorWorkloadResponse;
import com.example.backend.entity.Department;
import com.example.backend.entity.Mentor;
import com.example.backend.entity.User;
import com.example.backend.repository.DepartmentRepository;
import com.example.backend.repository.MentorRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.MentorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MentorServiceImpl implements MentorService {

    private final MentorRepository mentorRepository;

    // ✅ nếu createMentor cần tạo mentor theo userId/departmentId thì cần 2 repo này
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Page<MentorResponseDto> getMentors(Integer page, Integer size) {
        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size <= 0) ? 200 : Math.min(size, 500);

        Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "id"));
        Page<Mentor> mentors = mentorRepository.findAll(pageable);

        return mentors.map(this::toDto);
    }


    @Override
    @Transactional
    public MentorResponseDto createMentor(MentorCreateRequest request) {
        // ✅ Tùy field của request bạn đang có. Mình giả định:
        // request.getUserId(), request.getDepartmentId(), request.getTitle()

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + request.getUserId()));

        Department dept = null;
        if (request.getDepartmentId() != null) {
            dept = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found: " + request.getDepartmentId()));
        }

        Mentor mentor = new Mentor();
        mentor.setUser(user);
        mentor.setDepartment(dept);
        mentor.setTitle(request.getTitle());

        Mentor saved = mentorRepository.save(mentor);
        return toDto(saved);
    }

    private MentorResponseDto toDto(Mentor m) {
        User u = m.getUser();
        Department d = m.getDepartment();

        UserSummaryDto userDto = null;
        if (u != null) {
            userDto = new UserSummaryDto(u.getId(), u.getFullName(), u.getEmail());
        }

        DepartmentSummaryDto deptDto = null;
        if (d != null) {
            deptDto = new DepartmentSummaryDto(d.getId(), d.getCode(), d.getName());
        }

        return new MentorResponseDto(
                m.getId(),
                m.getTitle(),
                userDto,
                deptDto
        );
    }

    @Override
    public Page<MentorWorkloadResponse> getMentorWorkload(Integer page, Integer size, String search) {
        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size <= 0 || size > 200) ? 20 : size;

        Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "id"));
        return mentorRepository.findMentorWorkload(search, pageable);
    }
}
