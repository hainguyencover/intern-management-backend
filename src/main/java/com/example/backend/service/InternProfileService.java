package com.example.backend.service;

import com.example.backend.dto.InternSearchCriteria;
import com.example.backend.dto.request.InternProfileRequest;
import com.example.backend.dto.response.InternProfileResponse;
import com.example.backend.entity.InternProfile;
import com.example.backend.entity.Mentor;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.entity.GroupMember;
import com.example.backend.enums.GroupStatus;
import com.example.backend.enums.UserStatus;
import com.example.backend.exception.NotFoundException;
import com.example.backend.exception.ResourceNotFoundException;
import com.example.backend.repository.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternProfileService {

    private final InternProfileRepository internProfileRepository;
    private final UserRepository userRepository;
    private final MentorRepository mentorRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public InternProfileResponse createIntern(InternProfileRequest request) {
        User user;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + request.getUserId()));
        } else if (request.getEmail() != null && !request.getEmail().isBlank()) {
            Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
            if (userOpt.isPresent()) {
                user = userOpt.get();
            } else {
                // Auto-create user
                if (request.getFullName() == null || request.getFullName().isBlank()) {
                    throw new IllegalArgumentException("Full name is required for new user creation");
                }
                user = new User();
                user.setEmail(request.getEmail());
                user.setFullName(request.getFullName());
                user.setFullName(request.getFullName());

                // Use custom password if provided, otherwise generate random
                String passwordToUse;
                if (request.getPassword() != null && !request.getPassword().isBlank()) {
                    passwordToUse = request.getPassword();
                } else {
                    passwordToUse = UUID.randomUUID().toString().substring(0, 8);
                }

                user.setPasswordHash(passwordEncoder.encode(passwordToUse));
                user.setStatus(UserStatus.ACTIVE);

                Role internRole = roleRepository.findByCode("INTERN")
                        .orElseThrow(() -> new RuntimeException("Role INTERN not found"));
                user.setRoles(Set.of(internRole));

                user = userRepository.save(user);
                log.info("Auto-created user {} with password: {}", user.getEmail(), passwordToUse);
            }
        } else {
            throw new IllegalArgumentException("User ID or Email must be provided");
        }

        if (internProfileRepository.existsByUser_Id(user.getId())) {
            throw new RuntimeException("Intern profile already exists for user: " + user.getId());
        }

        InternProfile profile = new InternProfile();
        profile.setUser(user);
        profile.setStudentCode(request.getStudentCode());
        profile.setDob(request.getDob());
        profile.setUniversity(request.getUniversity());
        profile.setMajor(request.getMajor());
        profile.setPhone(request.getPhone());
        profile.setAddress(request.getAddress());
        profile.setGpa(request.getGpa());
        profile.setStartDate(request.getStartDate());
        profile.setEndDate(request.getEndDate());

        if (request.getMentorId() != null) {
            Mentor mentor = mentorRepository.findById(request.getMentorId())
                    .orElseThrow(() -> new RuntimeException("Mentor not found: " + request.getMentorId()));
            profile.setMentor(mentor);
        }

        profile = internProfileRepository.save(profile);
        log.info("Created intern profile for user: {}", user.getEmail());

        return mapToResponse(profile);
    }

    @Transactional
    public InternProfileResponse updateIntern(Long id, InternProfileRequest request) {
        InternProfile profile = internProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Intern profile not found: " + id));

        updateProfileData(profile, request);

        profile = internProfileRepository.save(profile);
        log.info("Updated intern profile: {}", id);

        return mapToResponse(profile);
    }

    @Transactional
    public void deleteIntern(Long id) {
        delete(id);
    }

    @Transactional(readOnly = true)
    public InternProfileResponse getInternProfileById(Long id) {
        InternProfile profile = internProfileRepository.findByIdWithUser(id)
                .orElseThrow(() -> new RuntimeException("Intern profile not found: " + id));
        return mapToResponse(profile);
    }

    @Transactional(readOnly = true)
    public InternProfileResponse getInternProfileByUserId(Long userId) {
        InternProfile profile = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Intern profile not found for user: " + userId));
        return mapToResponse(profile);
    }

    @Transactional
    public void delete(Long id) {
        if (!internProfileRepository.existsById(id)) {
            throw new NotFoundException("Intern profile", id);
        }
        internProfileRepository.deleteById(id);
        log.info("Deleted intern profile: {}", id);
    }

    @Transactional(readOnly = true)
    public InternProfileResponse getInternProfile(Long id) {
        InternProfile profile = internProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Intern profile not found"));
        return mapToResponse(profile);
    }

    @Transactional(readOnly = true)
    public InternProfileResponse getMyProfile(Long userId) {
        InternProfile profile = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Intern profile not found"));
        return mapToResponse(profile);
    }

    @Transactional(readOnly = true)
    public Page<InternProfileResponse> searchInterns(InternSearchCriteria criteria, Pageable pageable) {
        Specification<InternProfile> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getUniversity() != null && !criteria.getUniversity().trim().isEmpty()) {
                predicates.add(
                        cb.like(cb.lower(root.get("university")), "%" + criteria.getUniversity().toLowerCase() + "%"));
            }

            if (criteria.getMajor() != null && !criteria.getMajor().trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("major")), "%" + criteria.getMajor().toLowerCase() + "%"));
            }

            if (criteria.getMinGpa() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("gpa"), criteria.getMinGpa()));
            }

            if (criteria.getMaxGpa() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("gpa"), criteria.getMaxGpa()));
            }

            if (criteria.getMentorId() != null) {
                predicates.add(cb.equal(root.get("mentor").get("id"), criteria.getMentorId()));
            }

            if (criteria.getKeyword() != null && !criteria.getKeyword().trim().isEmpty()) {
                String likePattern = "%" + criteria.getKeyword().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("user").get("fullName")), likePattern),
                        cb.like(cb.lower(root.get("user").get("email")), likePattern),
                        cb.like(cb.lower(root.get("studentCode")), likePattern),
                        cb.like(cb.lower(root.get("university")), likePattern),
                        cb.like(cb.lower(root.get("major")), likePattern),
                        cb.like(root.get("gpa").as(String.class), likePattern)));
            }

            if (Boolean.TRUE.equals(criteria.getExcludeBusy())) {
                jakarta.persistence.criteria.Subquery<Long> subquery = query.subquery(Long.class);
                jakarta.persistence.criteria.Root<GroupMember> gm = subquery.from(GroupMember.class);
                subquery.select(gm.get("intern").get("id"));
                subquery.where(cb.equal(gm.get("group").get("status"), GroupStatus.ACTIVE));
                predicates.add(cb.not(root.get("id").in(subquery)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return internProfileRepository.findAll(spec, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<String> getAllUniversities() {
        return internProfileRepository.findAllUniversities();
    }

    @Transactional(readOnly = true)
    public List<String> getAllMajors() {
        return internProfileRepository.findAllMajors();
    }

    @Transactional
    public void deleteInternProfile(Long id) {
        InternProfile profile = internProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Intern profile not found: " + id));

        internProfileRepository.delete(profile);
        log.info("Deleted intern profile: {}", id);
    }

    private InternProfileResponse mapToResponse(InternProfile profile) {
        return InternProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .email(profile.getUser().getEmail())
                .fullName(profile.getUser().getFullName())
                .studentCode(profile.getStudentCode())
                .dob(profile.getDob())
                .university(profile.getUniversity())
                .major(profile.getMajor())
                .phone(profile.getPhone())
                .address(profile.getAddress())
                .gpa(profile.getGpa())
                .cvUrl(profile.getCvUrl())
                .startDate(profile.getStartDate())
                .endDate(profile.getEndDate())
                .mentorId(profile.getMentor() != null ? profile.getMentor().getId() : null)
                .mentorName(profile.getMentor() != null ? profile.getMentor().getUser().getFullName() : null)
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<InternProfileResponse> search(String university, String major, String keyword,
            int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<InternProfile> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (university != null && !university.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("university")), "%" + university.toLowerCase() + "%"));
            }

            if (major != null && !major.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("major")), "%" + major.toLowerCase() + "%"));
            }

            if (keyword != null && !keyword.isBlank()) {
                String likePattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("user").get("fullName")), likePattern),
                        cb.like(cb.lower(root.get("user").get("email")), likePattern),
                        cb.like(cb.lower(root.get("studentCode")), likePattern)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return internProfileRepository.findAll(spec, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public InternProfileResponse getById(Long id) {
        InternProfile profile = internProfileRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Intern profile", id));
        return toResponse(profile);
    }

    @Transactional
    public InternProfileResponse updateMyProfile(InternProfileRequest request, String email) {
        InternProfile profile = internProfileRepository.findByUser_Email(email)
                .orElseThrow(() -> new ResourceNotFoundException("Intern profile not found"));
        updateProfileData(profile, request);
        return mapToResponse(internProfileRepository.save(profile));
    }

    private void updateProfileData(InternProfile profile, InternProfileRequest request) {
        if (request.getStudentCode() != null)
            profile.setStudentCode(request.getStudentCode());
        if (request.getDob() != null)
            profile.setDob(request.getDob());
        if (request.getUniversity() != null)
            profile.setUniversity(request.getUniversity());
        if (request.getMajor() != null)
            profile.setMajor(request.getMajor());
        if (request.getPhone() != null)
            profile.setPhone(request.getPhone());
        if (request.getAddress() != null)
            profile.setAddress(request.getAddress());
        if (request.getGpa() != null)
            profile.setGpa(request.getGpa());
        if (request.getStartDate() != null)
            profile.setStartDate(request.getStartDate());
        if (request.getEndDate() != null)
            profile.setEndDate(request.getEndDate());

        if (request.getMentorId() != null) {
            Mentor mentor = mentorRepository.findById(request.getMentorId())
                    .orElseThrow(() -> new RuntimeException("Mentor not found: " + request.getMentorId()));
            profile.setMentor(mentor);
        }
    }

    @Transactional(readOnly = true)
    public InternProfileResponse getByUserId(Long userId) {
        InternProfile profile = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Intern profile not found for user: " + userId));
        return toResponse(profile);
    }

    private InternProfileResponse toResponse(InternProfile profile) {
        InternProfileResponse response = new InternProfileResponse();
        response.setId(profile.getId());
        response.setUserId(profile.getUser().getId());
        response.setEmail(profile.getUser().getEmail());
        response.setFullName(profile.getUser().getFullName());
        response.setPhone(profile.getUser().getPhone());
        response.setStudentCode(profile.getStudentCode());
        response.setDob(profile.getDob());
        response.setUniversity(profile.getUniversity());
        response.setMajor(profile.getMajor());
        response.setAddress(profile.getAddress());
        response.setGpa(profile.getGpa());
        response.setCvUrl(profile.getCvUrl());
        response.setStartDate(profile.getStartDate());
        response.setEndDate(profile.getEndDate());

        if (profile.getMentor() != null) {
            response.setMentorId(profile.getMentor().getId());
            response.setMentorName(profile.getMentor().getUser().getFullName());
        }

        response.setCreatedAt(profile.getCreatedAt());
        response.setUpdatedAt(profile.getUpdatedAt());

        return response;
    }

    @Transactional
    public void assignMentorByUserId(Long internId, Long mentorUserId) {
        InternProfile profile = internProfileRepository.findById(internId)
                .orElseThrow(() -> new NotFoundException("Intern profile", internId));

        if (mentorUserId == null) {
            profile.setMentor(null);
        } else {
            // Find Mentor profile OR auto-create if user is MENTOR but profile missing
            Mentor mentor = mentorRepository.findByUser_Id(mentorUserId)
                    .orElseGet(() -> {
                        // Check if user exists and should have a profile
                        User user = userRepository.findById(mentorUserId)
                                .orElseThrow(() -> new NotFoundException("User not found", mentorUserId));

                        boolean isMentorRole = user.getRoles().stream()
                                .anyMatch(r -> "MENTOR".equalsIgnoreCase(r.getCode()));

                        if (isMentorRole) {
                            log.warn("Mentor profile missing for user {}, auto-creating...", mentorUserId);
                            Mentor newMentor = new Mentor();
                            newMentor.setUser(user);
                            newMentor.setTitle("Mentor"); // Default title
                            return mentorRepository.save(newMentor);
                        } else {
                            throw new NotFoundException("Mentor profile for user", mentorUserId);
                        }
                    });

            profile.setMentor(mentor);
        }
        internProfileRepository.save(profile);
        log.info("Assigned mentor (User ID: {}) to intern {}", mentorUserId, internId);
    }

    @Transactional(readOnly = true)
    public List<com.example.backend.dto.InternCountStatDto> getInternStatsByUniversity() {
        return internProfileRepository.countInternsGroupedByUniversity();
    }

    @Transactional(readOnly = true)
    public List<com.example.backend.dto.InternCountStatDto> getInternStatsByMajor() {
        return internProfileRepository.countInternsGroupedByMajor();
    }
}
