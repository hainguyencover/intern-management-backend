package com.holaho.intern.service;

import com.holaho.intern.repository.GroupMemberRepository;
import com.holaho.intern.repository.InternProfileRepository;
import com.holaho.intern.repository.MentorRepository;
import com.holaho.intern.repository.RoleRepository;
import com.holaho.intern.repository.UserRepository;
import com.holaho.intern.shared.dto.InternCountStatDto;
import com.holaho.intern.shared.dto.response.CvScreeningResponse;
import com.holaho.intern.shared.mapper.InternMapper;


import com.holaho.intern.shared.dto.InternSearchCriteria;
import com.holaho.intern.shared.dto.request.InternProfileRequest;
import com.holaho.intern.shared.dto.response.InternProfileResponse;
import com.holaho.intern.entity.InternProfile;
import com.holaho.intern.entity.Mentor;
import com.holaho.intern.entity.Role;
import com.holaho.intern.entity.User;
import com.holaho.intern.entity.GroupMember;
import com.holaho.intern.shared.enums.GroupStatus;
import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.shared.exception.ResourceNotFoundException;
import com.holaho.intern.shared.elasticsearch.service.SearchService;
import com.holaho.intern.service.AiService;
import com.holaho.intern.service.InternProfileService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternProfileServiceImpl implements InternProfileService {

    private final InternProfileRepository internProfileRepository;
    private final UserRepository userRepository;
    private final MentorRepository mentorRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final GroupMemberRepository groupMemberRepository;
    private final com.holaho.intern.shared.mapper.InternMapper internMapper;
    private final SearchService searchService;
    private final AiService aiService;

    @Override
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
                if (request.getFullName() == null || request.getFullName().isBlank()) {
                    throw new IllegalArgumentException("Full name is required for new user creation");
                }
                user = new User();
                user.setEmail(request.getEmail());
                user.setFullName(request.getFullName());

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
        searchService.indexIntern(profile);
        log.info("Created intern profile for user: {}", user.getEmail());

        return mapToResponse(profile);
    }

    @Override
    @Transactional
    @CacheEvict(value = { "universities", "majors" }, allEntries = true)
    public InternProfileResponse updateIntern(Long id, InternProfileRequest request) {
        InternProfile profile = internProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Intern profile not found: " + id));

        updateProfileData(profile, request);

        profile = internProfileRepository.save(profile);
        searchService.indexIntern(profile);
        log.info("Updated intern profile: {}", id);

        return mapToResponse(profile);
    }

    @Override
    @Transactional
    public void deleteIntern(Long id) {
        delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public InternProfileResponse getInternProfileById(Long id) {
        InternProfile profile = internProfileRepository.findByIdWithUser(id)
                .orElseThrow(() -> new RuntimeException("Intern profile not found: " + id));
        return mapToResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public InternProfileResponse getInternProfileByUserId(Long userId) {
        InternProfile profile = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new RuntimeException("Intern profile not found for user: " + userId));
        return mapToResponse(profile);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!internProfileRepository.existsById(id)) {
            throw new NotFoundException("Intern profile", id);
        }
        internProfileRepository.deleteById(id);
        searchService.deleteIntern(id);
        log.info("Deleted intern profile: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public InternProfileResponse getInternProfile(Long id) {
        InternProfile profile = internProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Intern profile not found"));
        return mapToResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public InternProfileResponse getMyProfile(Long userId) {
        InternProfile profile = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Intern profile not found"));
        return mapToResponse(profile);
    }

    @Override
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

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "universities")
    public List<String> getAllUniversities() {
        return internProfileRepository.findAllUniversities();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "majors")
    public List<String> getAllMajors() {
        return internProfileRepository.findAllMajors();
    }

    @Override
    @Transactional
    public void deleteInternProfile(Long id) {
        InternProfile profile = internProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Intern profile not found: " + id));

        internProfileRepository.delete(profile);
        searchService.deleteIntern(id);
        log.info("Deleted intern profile: {}", id);
    }

    @Override
    @Transactional
    public InternProfileResponse updateMyProfile(InternProfileRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));

        InternProfile profile = internProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new NotFoundException("Intern profile not found"));

        updateProfileData(profile, request);

        profile = internProfileRepository.save(profile);
        searchService.indexIntern(profile);
        return mapToResponse(profile);
    }

    @Override
    @Transactional
    public void assignMentorByUserId(Long internId, Long mentorUserId) {
        InternProfile profile = internProfileRepository.findById(internId)
                .orElseThrow(() -> new NotFoundException("Intern profile", internId));

        Mentor mentor = null;
        if (mentorUserId != null) {
            mentor = mentorRepository.findByUser_Id(mentorUserId)
                    .orElseThrow(() -> new NotFoundException("Mentor profile for user", mentorUserId));
        }

        profile.setMentor(mentor);
        internProfileRepository.save(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.holaho.intern.shared.dto.InternCountStatDto> getInternStatsByUniversity() {
        return internProfileRepository.countInternsGroupedByUniversity();
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.holaho.intern.shared.dto.InternCountStatDto> getInternStatsByMajor() {
        return internProfileRepository.countInternsGroupedByMajor();
    }

    private InternProfileResponse mapToResponse(InternProfile profile) {
        GroupMember member = groupMemberRepository
                .findFirstByIntern_IdAndLeftAtIsNull(profile.getId()).orElse(null);
        return internMapper.toResponse(profile, member);
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

        if (request.getCvUrl() != null && !request.getCvUrl().equals(profile.getCvUrl())) {
            try {
                log.info("Triggering AI CV Screening for intern profile ID: {}", profile.getId());
                com.holaho.intern.shared.dto.response.CvScreeningResponse screening = aiService.screenCv(new byte[0],
                        "intern_cv.pdf");
                profile.setCvSkills(String.join(", ", screening.getSkills()));
                profile.setCvScore(screening.getScore());
                profile.setCvSummary(screening.getSummary());
            } catch (Exception e) {
                log.error("AI CV Screening failed", e);
            }
        }
        if (request.getCvUrl() != null)
            profile.setCvUrl(request.getCvUrl());

        if (request.getMentorId() != null) {
            Mentor mentor = mentorRepository.findById(request.getMentorId())
                    .orElseThrow(() -> new RuntimeException("Mentor not found: " + request.getMentorId()));
            profile.setMentor(mentor);
        }
    }
}

