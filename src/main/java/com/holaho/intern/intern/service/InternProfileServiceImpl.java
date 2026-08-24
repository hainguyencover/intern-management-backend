package com.holaho.intern.intern.service;

import com.holaho.intern.repository.GroupMemberRepository;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.user.repository.RoleRepository;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.shared.dto.InternCountStatDto;
import com.holaho.intern.shared.dto.response.CvScreeningResponse;
import com.holaho.intern.shared.mapper.InternMapper;

import com.holaho.intern.shared.dto.InternSearchCriteria;
import com.holaho.intern.shared.dto.request.InternProfileRequest;
import com.holaho.intern.shared.dto.response.InternProfileResponse;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.user.entity.Role;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.entity.GroupMember;
import com.holaho.intern.shared.enums.GroupStatus;
import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.shared.elasticsearch.service.SearchService;
import com.holaho.intern.service.AiService;
import com.holaho.intern.intern.service.InternProfileService;
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

import com.holaho.intern.service.AuditLogService;
import com.holaho.intern.intern.repository.specification.InternProfileSpecification;
import com.holaho.intern.shared.config.TenantContext;

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
    private final InternMapper internMapper;
    private final SearchService searchService;
    private final AiService aiService;
    private final AuditLogService auditLogService;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    @Override
    @Transactional
    public InternProfileResponse createIntern(InternProfileRequest request) {
        User user;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new NotFoundException("User không tồn tại: " + request.getUserId()));
        } else if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String cleanEmail = request.getEmail().trim().toLowerCase();
            Optional<User> userOpt = userRepository.findByEmail(cleanEmail);
            if (userOpt.isPresent()) {
                user = userOpt.get();
            } else {
                // Auto-provision User account with INTERN role if not exists
                Role internRole = roleRepository.findByCode("INTERN")
                        .orElseGet(() -> {
                            Role r = new Role();
                            r.setCode("INTERN");
                            r.setName("Intern / Candidate");
                            return roleRepository.save(r);
                        });

                user = new User();
                user.setEmail(cleanEmail);
                user.setFullName(request.getFullName() != null && !request.getFullName().isBlank() ? request.getFullName() : cleanEmail);
                user.setPasswordHash(passwordEncoder.encode("Intern@123456"));
                user.setStatus(UserStatus.ACTIVE);
                user.setTenantId(TenantContext.getCurrentTenantId() != null ? TenantContext.getCurrentTenantId() : 1L);
                user.setRoles(Set.of(internRole));

                user = userRepository.save(user);
                log.info("Auto-created User account for email: {} with INTERN role", user.getEmail());
            }
        } else {
            throw new BadRequestException("Cần cung cấp User ID hoặc Email người dùng hợp lệ.");
        }

        if (internProfileRepository.existsByUser_Id(user.getId())) {
            throw new ConflictException("Hồ sơ thực tập sinh đã tồn tại cho user: " + user.getId());
        }

        InternProfile profile = new InternProfile();
        profile.setUser(user);

        String studentCode = request.getStudentCode();
        if (studentCode == null || studentCode.isBlank()) {
            studentCode = generateNextStudentCode();
        }
        profile.setStudentCode(studentCode);

        profile.setDob(request.getDob());
        profile.setUniversity(request.getUniversity());
        profile.setMajor(request.getMajor());
        profile.setPhone(request.getPhone());
        profile.setAddress(request.getAddress());
        profile.setGpa(request.getGpa());
        profile.setStartDate(request.getStartDate());
        profile.setEndDate(request.getEndDate());
        profile.setStatus("DRAFT"); // Mặc định hồ sơ mới tạo ở trạng thái DRAFT

        if (request.getMentorId() != null) {
            Mentor mentor = mentorRepository.findById(request.getMentorId())
                    .orElseThrow(() -> new NotFoundException("Mentor không tồn tại: " + request.getMentorId()));
            profile.setMentor(mentor);
        }

        profile = internProfileRepository.save(profile);
        searchService.indexIntern(profile);
        log.info("Created intern profile ID: {} linked to user: {}", profile.getId(), user.getEmail());

        // Audit Logging for US-001 & US-048
        if (auditLogService != null) {
            try {
                String currentActorEmail = "system";
                if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null) {
                    currentActorEmail = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
                }
                auditLogService.createAuditLog(
                        null,
                        currentActorEmail,
                        "CREATE",
                        "INTERN_PROFILE",
                        profile.getId(),
                        "SUCCESS",
                        "Khởi tạo hồ sơ thực tập sinh cho email: " + user.getEmail(),
                        null,
                        null,
                        null,
                        null
                );
            } catch (Exception e) {
                log.warn("Could not log audit event for intern profile creation: {}", e.getMessage());
            }
        }

        return mapToResponse(profile);
    }

    @Override
    @Transactional
    @CacheEvict(value = { "universities", "majors" }, allEntries = true)
    public InternProfileResponse updateIntern(Long id, InternProfileRequest request) {
        InternProfile profile = internProfileRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Hồ sơ thực tập sinh không tồn tại: " + id));

        // Business Rule US-002 & BR-05: Status COMPLETED and REJECTED are locked and cannot be edited
        if ("COMPLETED".equalsIgnoreCase(profile.getStatus())) {
            throw new ConflictException("Hồ sơ đã hoàn thành và không thể chỉnh sửa.");
        }
        if ("REJECTED".equalsIgnoreCase(profile.getStatus())) {
            throw new ConflictException("Hồ sơ đã bị từ chối và không thể chỉnh sửa. Theo BR-05, ứng viên cần khởi tạo hồ sơ mới.");
        }

        updateProfileData(profile, request);

        profile = internProfileRepository.save(profile);
        searchService.indexIntern(profile);
        log.info("Updated intern profile: {}", id);

        // Audit Logging for US-002 & US-048
        if (auditLogService != null) {
            try {
                String currentActorEmail = "system";
                if (org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null) {
                    currentActorEmail = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
                }
                auditLogService.createAuditLog(
                        null,
                        currentActorEmail,
                        "UPDATE",
                        "INTERN_PROFILE",
                        profile.getId(),
                        "SUCCESS",
                        "Chỉnh sửa thông tin hồ sơ thực tập sinh ID: " + profile.getId(),
                        null,
                        null,
                        null,
                        null
                );
            } catch (Exception e) {
                log.warn("Could not log audit event for intern profile update: {}", e.getMessage());
            }
        }

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
                .orElseThrow(() -> new NotFoundException("Hồ sơ thực tập sinh không tồn tại: " + id));
        return mapToResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public InternProfileResponse getInternProfileByUserId(Long userId) {
        InternProfile profile = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Hồ sơ thực tập sinh không tồn tại cho user: " + userId));
        return mapToResponse(profile);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        InternProfile profile = internProfileRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Hồ sơ thực tập sinh không tồn tại: " + id));

        try {
            // Clean up child table records linked to this intern
            entityManager.createNativeQuery("DELETE FROM group_members WHERE intern_id = :id").setParameter("id", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM evaluations WHERE intern_id = :id").setParameter("id", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM attendances WHERE intern_id = :id").setParameter("id", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM applications WHERE intern_id = :id").setParameter("id", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM weekly_reports WHERE intern_id = :id").setParameter("id", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM leave_requests WHERE intern_id = :id").setParameter("id", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM allowances WHERE intern_id = :id").setParameter("id", id).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM intern_documents WHERE intern_id = :id").setParameter("id", id).executeUpdate();

            internProfileRepository.delete(profile);
            internProfileRepository.flush();
            searchService.deleteIntern(id);
            log.info("Deleted intern profile ID: {}", id);
        } catch (Exception e) {
            log.error("Failed to delete intern profile ID {}", id, e);
            throw new ConflictException("Không thể xóa hồ sơ thực tập sinh này: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InternProfileResponse getInternProfile(Long id) {
        InternProfile profile = internProfileRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Hồ sơ thực tập sinh không tồn tại: " + id));
        return mapToResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public InternProfileResponse getMyProfile(Long userId) {
        InternProfile profile = internProfileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Hồ sơ thực tập sinh không tồn tại cho user: " + userId));
        return mapToResponse(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InternProfileResponse> searchInterns(InternSearchCriteria criteria, Pageable pageable) {
        boolean hasFilters = criteria != null && (
                (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) ||
                (criteria.getUniversity() != null && !criteria.getUniversity().isBlank()) ||
                (criteria.getMajor() != null && !criteria.getMajor().isBlank()) ||
                (criteria.getStatus() != null && !criteria.getStatus().isBlank()) ||
                Boolean.TRUE.equals(criteria.getExcludeBusy())
        );

        Page<InternProfile> pageResult;
        if (!hasFilters) {
            // Default initial load: Fetch all profiles directly without spec filtering
            pageResult = internProfileRepository.findAll(pageable);
        } else {
            Long tenantId = TenantContext.getCurrentTenantId();
            Specification<InternProfile> spec = InternProfileSpecification.buildSpecification(criteria, tenantId);
            pageResult = internProfileRepository.findAll(spec, pageable);
        }

        return pageResult.map(this::mapToResponse);
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
                .orElseThrow(() -> new NotFoundException("Hồ sơ thực tập sinh không tồn tại: " + id));

        internProfileRepository.delete(profile);
        searchService.deleteIntern(id);
        log.info("Deleted intern profile: {}", id);
    }

    @Override
    @Transactional
    public InternProfileResponse updateMyProfile(InternProfileRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User không tồn tại: " + email));

        InternProfile profile = internProfileRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new NotFoundException("Hồ sơ thực tập sinh không tồn tại"));

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
    @Transactional
    public void updateInternStatus(Long id, String status) {
        InternProfile profile = internProfileRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Hồ sơ thực tập sinh không tồn tại: " + id));
        profile.setStatus(status);
        internProfileRepository.save(profile);
        log.info("Updated status of intern profile {} to {}", id, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InternCountStatDto> getInternStatsByUniversity() {
        return internProfileRepository.countInternsGroupedByUniversity();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InternCountStatDto> getInternStatsByMajor() {
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
                CvScreeningResponse screening = aiService.screenCv(new byte[0],
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
                    .orElseThrow(() -> new NotFoundException("Mentor không tồn tại: " + request.getMentorId()));
            profile.setMentor(mentor);
        }
    }

    private synchronized String generateNextStudentCode() {
        int year = java.time.Year.now().getValue();
        String prefix = "TTS" + year + "-";

        List<InternProfile> existing = internProfileRepository.findAll();
        long highest = 0;
        for (InternProfile p : existing) {
            if (p.getStudentCode() != null && p.getStudentCode().startsWith(prefix)) {
                try {
                    String seqStr = p.getStudentCode().substring(prefix.length());
                    long seq = Long.parseLong(seqStr);
                    if (seq > highest) {
                        highest = seq;
                    }
                } catch (Exception ignored) {}
            }
        }

        long nextSeq = Math.max(highest + 1, existing.size() + 1);
        return String.format("%s%03d", prefix, nextSeq);
    }
}
