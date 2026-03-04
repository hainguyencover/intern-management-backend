package com.example.backend.service.impl;

import com.example.backend.dto.request.CreateMentorRequest;
import com.example.backend.dto.response.*;
import com.example.backend.entity.*;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.NotFoundException;
import com.example.backend.mapper.*;
import com.example.backend.repository.*;
import com.example.backend.service.MentorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final TaskRepository taskRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final InternProfileRepository internProfileRepository;
    private final GroupMemberRepository groupMemberRepository;

    private final MentorMapper mentorMapper;
    private final TaskMapper taskMapper;
    private final WeeklyReportMapper weeklyReportMapper;
    private final InternMapper internMapper;

    @Override
    @Transactional
    public MentorResponse createMentor(CreateMentorRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new NotFoundException("User", request.getUserId()));

        if (mentorRepository.existsByUser_Id(request.getUserId())) {
            throw new ConflictException("Mentor profile đã tồn tại cho user: " + request.getUserId());
        }

        Department department = null;
        if (request.getDepartmentId() != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new NotFoundException("Department", request.getDepartmentId()));
        }

        Mentor mentor = new Mentor();
        mentor.setUser(user);
        mentor.setDepartment(department);
        mentor.setTitle(request.getTitle());

        mentor = mentorRepository.save(mentor);
        log.info("Created mentor profile for user: {}", user.getEmail());

        return mapToResponse(mentor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentorResponse> getAllMentors() {
        return mentorRepository.findAllWithUserExcludingInterns().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MentorResponse> getMentors(Pageable pageable) {
        return mentorRepository.findMentorsExcludingInterns(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public MentorResponse getMentorById(Long id) {
        Mentor mentor = mentorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Mentor", id));
        return mapToResponse(mentor);
    }

    @Override
    @Transactional(readOnly = true)
    public MentorResponse getMentorByUserId(Long userId) {
        Mentor mentor = mentorRepository.findByUser_Id(userId)
                .orElseThrow(() -> new NotFoundException("Mentor not found for user: " + userId));
        return mapToResponse(mentor);
    }

    @Override
    @Transactional(readOnly = true)
    public MentorDashboardStats getDashboardStats(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));

        Mentor mentor = mentorRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new NotFoundException("Mentor profile not found for user: " + email));

        long totalInterns = internProfileRepository.countByMentorId(mentor.getId());
        long activeTasks = taskRepository.countByCreatedByIdAndStatusNot(user.getId(),
                com.example.backend.enums.TaskStatus.DONE);
        long reviewedReports = weeklyReportRepository.countByMentor_IdAndStatus(user.getId(),
                com.example.backend.enums.WeeklyReportStatus.REVIEWED.name());

        List<TaskResponse> recentTasks = taskRepository
                .findTop5ByCreatedByIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(taskMapper::toResponse)
                .collect(Collectors.toList());

        List<com.example.backend.dto.WeeklyReportDto> recentReports = weeklyReportRepository
                .findTop5ByMentor_IdOrderByWeekNumberDesc(user.getId())
                .stream()
                .map(weeklyReportMapper::toDto)
                .collect(Collectors.toList());

        return MentorDashboardStats.builder()
                .totalInterns(totalInterns)
                .activeTasks(activeTasks)
                .reviewedReports(reviewedReports)
                .recentTasks(recentTasks)
                .recentReports(recentReports)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InternProfileResponse> getAssignedInterns(String email, String keyword, String status,
            Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));

        Mentor mentor = mentorRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new NotFoundException("Mentor profile not found for user: " + email));

        Specification<InternProfile> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String likePattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("user").get("fullName")), likePattern),
                        cb.like(cb.lower(root.get("user").get("email")), likePattern),
                        cb.like(cb.lower(root.get("studentCode")), likePattern)));
            }

            jakarta.persistence.criteria.Subquery<Long> subquery = query.subquery(Long.class);
            jakarta.persistence.criteria.Root<GroupMember> gmRoot = subquery.from(GroupMember.class);
            subquery.select(gmRoot.get("intern").get("id"));
            subquery.where(
                    cb.equal(gmRoot.get("group").get("mentorId"), mentor.getId()),
                    cb.isNull(gmRoot.get("leftAt")));

            predicates.add(root.get("id").in(subquery));

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
    public MentorResponse updateMentor(Long id, CreateMentorRequest request) {
        Mentor mentor = mentorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Mentor", id));

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new NotFoundException("Department", request.getDepartmentId()));
            mentor.setDepartment(department);
        }

        mentor.setTitle(request.getTitle());
        mentor = mentorRepository.save(mentor);
        log.info("Updated mentor: {}", mentor.getId());

        return mapToResponse(mentor);
    }

    private MentorResponse mapToResponse(Mentor mentor) {
        long internCount = internProfileRepository.countByMentorId(mentor.getId());
        return mentorMapper.toResponse(mentor, internCount);
    }

    private InternProfileResponse mapToInternProfileResponse(InternProfile profile) {
        GroupMember member = groupMemberRepository
                .findFirstByIntern_IdAndLeftAtIsNull(profile.getId()).orElse(null);
        return internMapper.toResponse(profile, member);
    }
}
