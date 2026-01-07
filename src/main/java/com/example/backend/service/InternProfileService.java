package com.example.backend.service;

import com.example.backend.dto.request.InternProfileRequest;
import com.example.backend.dto.response.InternProfileResponse;
import com.example.backend.dto.response.PageResponse;
import com.example.backend.entity.*;
import com.example.backend.enums.UserStatus;
import com.example.backend.exception.ApiException;
import com.example.backend.repository.GroupMemberRepository;
import com.example.backend.repository.InternProfileRepository;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.spec.InternSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InternProfileService {

    private final InternProfileRepository internProfileRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final GroupMemberRepository groupMemberRepository;

    @Transactional(readOnly = true)
    public PageResponse<InternProfileResponse> searchInterns(
            String search, String university, String major,
            int page, int size, String sortBy, String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        var spec = InternSpecifications.filter(search, university, major);
        Page<InternProfile> profiles = internProfileRepository.findAll(spec, pageable);

        Page<InternProfileResponse> respPage = profiles.map(this::toResponse);
        return PageResponse.of(respPage);
    }

    @Transactional
    public InternProfileResponse createIntern(InternProfileRequest req) {
        if (req.getStartDate() != null && req.getEndDate() != null && req.getStartDate().isAfter(req.getEndDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "startDate must be <= endDate");
        }

        String email = req.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already exists");
        }

        User user = new User();
        user.setEmail(email);
        user.setFullName(req.getFullName().trim());
        user.setPhone(req.getPhone());
        user.setStatus(UserStatus.ACTIVE);

        String tempPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        user.setPasswordHash(passwordEncoder.encode(tempPassword));

        Role internRole = roleRepository.findByCode("INTERN")
                .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Role INTERN missing"));
        user.getRoles().add(internRole);

        user = userRepository.save(user);

        InternProfile ip = new InternProfile();
        ip.setUser(user);
        ip.setStudentCode(req.getStudentCode());
        ip.setDob(req.getDob());
        ip.setUniversity(req.getUniversity());
        ip.setMajor(req.getMajor());
        ip.setPhone(req.getPhone());
        ip.setAddress(req.getAddress());
        ip.setGpa(req.getGpa());
        ip.setCvUrl(req.getCvUrl());
        ip.setStartDate(req.getStartDate());
        ip.setEndDate(req.getEndDate());

        InternProfile saved = internProfileRepository.save(ip);
        return toResponse(saved);
    }

    @Transactional
    public InternProfileResponse createForCurrentUser(InternProfileRequest req) {
        if (req.getStartDate() != null && req.getEndDate() != null && req.getStartDate().isAfter(req.getEndDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "startDate must be <= endDate");
        }

        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(principal)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + principal));

        // check if profile already exists
        if (internProfileRepository.findByUser_Id(user.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Intern profile already exists for user");
        }

        InternProfile ip = new InternProfile();
        ip.setUser(user);
        ip.setStudentCode(req.getStudentCode());
        ip.setDob(req.getDob());
        ip.setUniversity(req.getUniversity());
        ip.setMajor(req.getMajor());
        ip.setPhone(req.getPhone());
        ip.setAddress(req.getAddress());
        ip.setGpa(req.getGpa());
        ip.setCvUrl(req.getCvUrl());
        ip.setStartDate(req.getStartDate());
        ip.setEndDate(req.getEndDate());

        InternProfile saved = internProfileRepository.save(ip);
        // update user's fullName and phone if provided
        if (req.getFullName() != null) {
            user.setFullName(req.getFullName().trim());
        }
        if (req.getPhone() != null) {
            user.setPhone(req.getPhone());
        }
        userRepository.save(user);

        return toResponse(saved);
    }

    @Transactional
    public InternProfileResponse updateForCurrentUser(InternProfileRequest req) {
        if (req.getStartDate() != null && req.getEndDate() != null && req.getStartDate().isAfter(req.getEndDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "startDate must be <= endDate");
        }

        String principal = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(principal)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found: " + principal));

        InternProfile ip = internProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "Intern profile not found for user: " + principal));

        // update user fields
        String newEmail = req.getEmail().trim().toLowerCase();
        if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already exists");
        }
        user.setEmail(newEmail);
        user.setFullName(req.getFullName().trim());
        user.setPhone(req.getPhone());
        userRepository.save(user);

        ip.setStudentCode(req.getStudentCode());
        ip.setDob(req.getDob());
        ip.setUniversity(req.getUniversity());
        ip.setMajor(req.getMajor());
        ip.setPhone(req.getPhone());
        ip.setAddress(req.getAddress());
        ip.setGpa(req.getGpa());
        ip.setCvUrl(req.getCvUrl());
        ip.setStartDate(req.getStartDate());
        ip.setEndDate(req.getEndDate());

        InternProfile saved = internProfileRepository.save(ip);
        return toResponse(saved);
    }

    @Transactional
    public InternProfileResponse updateIntern(Long id, InternProfileRequest req) {
        if (req.getStartDate() != null && req.getEndDate() != null && req.getStartDate().isAfter(req.getEndDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "startDate must be <= endDate");
        }

        InternProfile ip = internProfileRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Intern not found: " + id));

        User user = ip.getUser();

        String newEmail = req.getEmail().trim().toLowerCase();
        if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already exists");
        }

        user.setEmail(newEmail);
        user.setFullName(req.getFullName().trim());
        user.setPhone(req.getPhone());
        userRepository.save(user);

        ip.setStudentCode(req.getStudentCode());
        ip.setDob(req.getDob());
        ip.setUniversity(req.getUniversity());
        ip.setMajor(req.getMajor());
        ip.setPhone(req.getPhone());
        ip.setAddress(req.getAddress());
        ip.setGpa(req.getGpa());
        ip.setCvUrl(req.getCvUrl());
        ip.setStartDate(req.getStartDate());
        ip.setEndDate(req.getEndDate());

        InternProfile saved = internProfileRepository.save(ip);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public InternProfileResponse getInternById(Long id) {
        InternProfile ip = internProfileRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Intern not found: " + id));
        return toResponse(ip);
    }

    @Transactional
    public void deleteIntern(Long id) {
        InternProfile ip = intern_profile_or_404(id);
        internProfileRepository.delete(ip);
        userRepository.delete(ip.getUser());
    }

    @Transactional(readOnly = true)
    public List<String> getAllUniversities() {
        return internProfileRepository.findAllUniversities();
    }

    @Transactional(readOnly = true)
    public List<String> getAllMajors() {
        return internProfileRepository.findAllMajors();
    }

    private InternProfile intern_profile_or_404(Long id) {
        return internProfileRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Intern not found: " + id));
    }

    private InternProfileResponse toResponse(InternProfile ip) {
        InternProfileResponse r = new InternProfileResponse();
        r.setInternId(ip.getId());
        User u = ip.getUser();
        r.setUserId(u != null ? u.getId() : null);
        if (u != null) {
            r.setFullName(u.getFullName());
            r.setEmail(u.getEmail());
            // prefer profile phone if present
            r.setPhone(ip.getPhone() != null ? ip.getPhone() : u.getPhone());
            r.setCreatedAt(u.getCreatedAt());
            r.setUpdatedAt(u.getUpdatedAt());
        }
        r.setStudentCode(ip.getStudentCode());
        r.setDob(ip.getDob());
        r.setUniversity(ip.getUniversity());
        r.setMajor(ip.getMajor());
        r.setAddress(ip.getAddress());
        r.setGpa(ip.getGpa());
        r.setCvUrl(ip.getCvUrl());
        r.setStartDate(ip.getStartDate());
        r.setEndDate(ip.getEndDate());


        Long mentorId = groupMemberRepository
                .findFirstByIntern_IdAndLeftAtIsNull(ip.getId())
                .map(GroupMember::getGroup)
                .map(ProgramGroup::getMentorId)
                .orElse(null);

        r.setMentorId(mentorId);

        return r;
    }
}
