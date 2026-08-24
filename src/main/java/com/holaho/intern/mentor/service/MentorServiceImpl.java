package com.holaho.intern.mentor.service;

import com.holaho.intern.entity.Department;
import com.holaho.intern.entity.GroupMember;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.mentor.dto.CreateMentorRequest;
import com.holaho.intern.mentor.dto.MentorStatusUpdateRequest;
import com.holaho.intern.mentor.dto.UpdateMentorRequest;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.mentor.exception.MentorHasActiveAssignmentsException;
import com.holaho.intern.mentor.exception.MentorNotFoundException;
import com.holaho.intern.mentor.repository.MentorAssignmentRepository;
import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.repository.DepartmentRepository;
import com.holaho.intern.repository.GroupMemberRepository;
import com.holaho.intern.repository.WeeklyReportRepository;
import com.holaho.intern.shared.config.TenantContext;
import com.holaho.intern.shared.dto.WeeklyReportDto;
import com.holaho.intern.shared.dto.request.MentorAssignmentRequest;
import com.holaho.intern.shared.dto.response.InternProfileResponse;
import com.holaho.intern.shared.dto.response.MentorDashboardStats;
import com.holaho.intern.shared.dto.response.MentorResponse;
import com.holaho.intern.shared.dto.response.TaskResponse;
import com.holaho.intern.shared.enums.MentorAssignmentStatus;
import com.holaho.intern.shared.enums.MentorStatus;
import com.holaho.intern.shared.enums.TaskStatus;
import com.holaho.intern.shared.enums.WeeklyReportStatus;
import com.holaho.intern.shared.exception.ConflictException;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.shared.mapper.InternMapper;
import com.holaho.intern.shared.mapper.TaskMapper;
import com.holaho.intern.shared.mapper.WeeklyReportMapper;
import com.holaho.intern.task.repository.TaskRepository;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MentorServiceImpl implements MentorService {

    private final MentorRepository mentorRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final TaskRepository taskRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final InternProfileRepository internProfileRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final PasswordEncoder passwordEncoder;

    private final TaskMapper taskMapper;
    private final WeeklyReportMapper weeklyReportMapper;
    private final InternMapper internMapper;

    private Long getCurrentTenantId() {
        Long tenantId = TenantContext.getCurrentTenantId();
        return tenantId != null ? tenantId : 1L;
    }

    @Override
    @Transactional
    public MentorResponse createMentor(CreateMentorRequest request) {
        Long tenantId = getCurrentTenantId();

        if (mentorRepository.existsByTenantIdAndEmployeeCode(tenantId, request.employeeCode())) {
            throw new ConflictException("Employee code already exists in tenant: " + request.employeeCode());
        }

        User user = null;
        if (request.userId() != null) {
            user = userRepository.findById(request.userId())
                    .orElseThrow(() -> new NotFoundException("User", request.userId()));
            if (mentorRepository.existsByUser_Id(user.getId())) {
                throw new ConflictException("Mentor profile already exists for user ID: " + request.userId());
            }
        } else {
            // Find existing user by email or create new user
            user = userRepository.findByEmail(request.email()).orElse(null);
            if (user == null) {
                user = new User();
                user.setEmail(request.email());
                user.setFullName(request.fullName());
                user.setPhone(request.phone());
                user.setStatus(com.holaho.intern.shared.enums.UserStatus.ACTIVE);
                String rawPassword = request.initialPassword() != null ? request.initialPassword() : "Mentor123@Password";
                user.setPasswordHash(passwordEncoder.encode(rawPassword));
                user.setTenantId(tenantId);
                user = userRepository.save(user);
                log.info("Created user account for mentor: {}", user.getEmail());
            }
        }

        Department department = null;
        if (request.departmentId() != null) {
            department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new NotFoundException("Department", request.departmentId()));
        }

        Mentor mentor = Mentor.builder()
                .user(user)
                .employeeCode(request.employeeCode())
                .fullName(request.fullName())
                .phone(request.phone())
                .department(department)
                .position(request.position())
                .title(request.title())
                .specialization(request.specialization())
                .yearsOfExperience(request.yearsOfExperience())
                .capacity(request.capacity() != null ? request.capacity() : 5)
                .status(MentorStatus.ACTIVE)
                .avatarUrl(request.avatarUrl())
                .bio(request.bio())
                .build();

        mentor.setTenantId(tenantId);
        mentor = mentorRepository.save(mentor);
        log.info("Created mentor profile ID: {} for user: {}", mentor.getId(), user.getEmail());

        return mapToResponse(mentor);
    }

    @Override
    @Transactional
    public MentorResponse updateMentor(Long id, UpdateMentorRequest request) {
        Long tenantId = getCurrentTenantId();
        Mentor mentor = mentorRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MentorNotFoundException(id));

        if (request.fullName() != null && !request.fullName().isBlank()) {
            mentor.setFullName(request.fullName());
            if (mentor.getUser() != null) {
                mentor.getUser().setFullName(request.fullName());
            }
        }
        if (request.phone() != null) {
            mentor.setPhone(request.phone());
            if (mentor.getUser() != null) {
                mentor.getUser().setPhone(request.phone());
            }
        }
        if (request.departmentId() != null) {
            Department department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new NotFoundException("Department", request.departmentId()));
            mentor.setDepartment(department);
        }
        if (request.position() != null) mentor.setPosition(request.position());
        if (request.title() != null) mentor.setTitle(request.title());
        if (request.specialization() != null) mentor.setSpecialization(request.specialization());
        if (request.yearsOfExperience() != null) mentor.setYearsOfExperience(request.yearsOfExperience());
        if (request.capacity() != null) mentor.setCapacity(request.capacity());
        if (request.status() != null) mentor.setStatus(request.status());
        if (request.avatarUrl() != null) mentor.setAvatarUrl(request.avatarUrl());
        if (request.bio() != null) mentor.setBio(request.bio());

        mentor = mentorRepository.save(mentor);
        log.info("Updated mentor profile ID: {}", id);
        return mapToResponse(mentor);
    }

    @Override
    @Transactional
    public MentorResponse updateMentorStatus(Long id, MentorStatusUpdateRequest request) {
        Long tenantId = getCurrentTenantId();
        Mentor mentor = mentorRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MentorNotFoundException(id));

        if (request.status() == MentorStatus.INACTIVE || request.status() == MentorStatus.SUSPENDED) {
            long activeAssignments = mentorAssignmentRepository.countByTenantIdAndMentorIdAndStatus(
                    tenantId, id, MentorAssignmentStatus.ACTIVE
            );
            if (activeAssignments > 0 && !request.forceDeactivate()) {
                throw new MentorHasActiveAssignmentsException(id, activeAssignments);
            }
        }

        mentor.setStatus(request.status());
        mentor = mentorRepository.save(mentor);
        log.info("Updated status of mentor ID: {} to {}", id, request.status());

        return mapToResponse(mentor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentorResponse> getAllMentors() {
        Long tenantId = getCurrentTenantId();
        return mentorRepository.findAllByTenantIdWithDetails(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MentorResponse> getMentors(Pageable pageable) {
        Long tenantId = getCurrentTenantId();
        return mentorRepository.findByTenantId(tenantId, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MentorResponse> searchMentors(String search, MentorStatus status, Long departmentId, Pageable pageable) {
        Long tenantId = getCurrentTenantId();
        return mentorRepository.searchMentors(tenantId, search, status, departmentId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public MentorResponse getMentorById(Long id) {
        Long tenantId = getCurrentTenantId();
        Mentor mentor = mentorRepository.findByTenantIdAndIdWithDetails(tenantId, id)
                .orElseThrow(() -> new MentorNotFoundException(id));
        return mapToResponse(mentor);
    }

    @Override
    @Transactional(readOnly = true)
    public MentorResponse getMentorByUserId(Long userId) {
        Long tenantId = getCurrentTenantId();
        Mentor mentor = mentorRepository.findByTenantIdAndUser_Id(tenantId, userId)
                .orElseThrow(() -> new NotFoundException("Mentor not found for user: " + userId));
        return mapToResponse(mentor);
    }

    @Override
    @Transactional(readOnly = true)
    public MentorDashboardStats getDashboardStats(String email) {
        Long tenantId = getCurrentTenantId();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));

        Mentor mentor = mentorRepository.findByTenantIdAndUser_Id(tenantId, user.getId()).orElse(null);
        Long mentorId = mentor != null ? mentor.getId() : null;

        long totalInterns = mentorId != null ? mentorAssignmentRepository.countByTenantIdAndMentorIdAndStatus(tenantId, mentorId, MentorAssignmentStatus.ACTIVE) : 0;
        long activeInterns = totalInterns;
        long completedInterns = mentorId != null ? mentorAssignmentRepository.countByTenantIdAndMentorIdAndStatus(tenantId, mentorId, MentorAssignmentStatus.COMPLETED) : 0;

        long pendingTasks = taskRepository.countByCreatedByIdAndStatusNot(user.getId(), TaskStatus.DONE);
        long pendingReports = weeklyReportRepository.countByMentor_IdAndStatus(user.getId(), WeeklyReportStatus.SUBMITTED.name());

        List<TaskResponse> recentTasks = taskRepository
                .findTop5ByCreatedByIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());

        List<WeeklyReportDto> recentReports = weeklyReportRepository
                .findTop5ByMentor_IdOrderByWeekNumberDesc(user.getId())
                .stream()
                .map(weeklyReportMapper::toDto)
                .collect(Collectors.toList());

        return MentorDashboardStats.builder()
                .totalInterns(totalInterns + completedInterns)
                .activeInterns(activeInterns)
                .completedInterns(completedInterns)
                .pendingReports(pendingReports)
                .overdueReports(0)
                .pendingTasks(pendingTasks)
                .averageInternProgress(85.0)
                .recentTasks(recentTasks)
                .recentReports(recentReports)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InternProfileResponse> getAssignedInterns(String email, String keyword, String status, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));

        Mentor mentor = mentorRepository.findByUser_Id(user.getId()).orElse(null);

        Specification<InternProfile> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String likePattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("user").get("fullName")), likePattern),
                        cb.like(cb.lower(root.get("user").get("email")), likePattern),
                        cb.like(cb.lower(root.get("studentCode")), likePattern)));
            }

            if (mentor != null) {
                jakarta.persistence.criteria.Subquery<Long> subquery = query.subquery(Long.class);
                jakarta.persistence.criteria.Root<GroupMember> gmRoot = subquery.from(GroupMember.class);
                subquery.select(gmRoot.get("intern").get("id"));
                subquery.where(
                        cb.equal(gmRoot.get("group").get("mentorId"), mentor.getId()),
                        cb.isNull(gmRoot.get("leftAt")));

                predicates.add(root.get("id").in(subquery));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return internProfileRepository.findAll(spec, pageable)
                .map(this::mapToInternProfileResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public InternProfileResponse getInternDetail(Long internId) {
        InternProfile profile = internProfileRepository.findById(internId)
                .orElseThrow(() -> new NotFoundException("Intern profile", internId));
        return mapToInternProfileResponse(profile);
    }

    @Override
    @Transactional
    public void deleteMentor(Long id) {
        Long tenantId = getCurrentTenantId();
        Mentor mentor = mentorRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new MentorNotFoundException(id));

        long activeAssignments = mentorAssignmentRepository.countByTenantIdAndMentorIdAndStatus(tenantId, id, MentorAssignmentStatus.ACTIVE);
        if (activeAssignments > 0) {
            throw new MentorHasActiveAssignmentsException(id, activeAssignments);
        }

        mentorRepository.delete(mentor);
        log.info("Deleted mentor ID: {}", id);
    }

    private MentorResponse mapToResponse(Mentor mentor) {
        int activeCount = (int) mentorAssignmentRepository.countByTenantIdAndMentorIdAndStatus(
                mentor.getTenantId(), mentor.getId(), MentorAssignmentStatus.ACTIVE
        );

        int capacity = mentor.getCapacity() != null ? mentor.getCapacity() : 5;
        int available = Math.max(0, capacity - activeCount);
        double utilization = capacity > 0 ? ((double) activeCount / capacity) * 100.0 : 0.0;

        return MentorResponse.builder()
                .id(mentor.getId())
                .userId(mentor.getUser() != null ? mentor.getUser().getId() : null)
                .email(mentor.getUser() != null ? mentor.getUser().getEmail() : null)
                .fullName(mentor.getFullName() != null ? mentor.getFullName() : (mentor.getUser() != null ? mentor.getUser().getFullName() : ""))
                .phone(mentor.getPhone() != null ? mentor.getPhone() : (mentor.getUser() != null ? mentor.getUser().getPhone() : null))
                .employeeCode(mentor.getEmployeeCode())
                .departmentId(mentor.getDepartment() != null ? mentor.getDepartment().getId() : null)
                .departmentName(mentor.getDepartment() != null ? mentor.getDepartment().getName() : null)
                .position(mentor.getPosition())
                .title(mentor.getTitle())
                .specialization(mentor.getSpecialization())
                .yearsOfExperience(mentor.getYearsOfExperience())
                .capacity(capacity)
                .activeInternsCount(activeCount)
                .availableCapacity(available)
                .capacityUtilizationPercentage(utilization)
                .status(mentor.getStatus())
                .avatarUrl(mentor.getAvatarUrl())
                .bio(mentor.getBio())
                .createdAt(mentor.getCreatedAt())
                .updatedAt(mentor.getUpdatedAt())
                .build();
    }

    private InternProfileResponse mapToInternProfileResponse(InternProfile profile) {
        GroupMember member = groupMemberRepository
                .findFirstByIntern_IdAndLeftAtIsNull(profile.getId()).orElse(null);
        return internMapper.toResponse(profile, member);
    }
}
