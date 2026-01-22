package com.example.backend.service;

import com.example.backend.dto.request.CreateMentorRequest;
import com.example.backend.dto.response.MentorResponse;
import com.example.backend.entity.Department;
import com.example.backend.entity.Mentor;
import com.example.backend.entity.User;
import com.example.backend.exception.ConflictException;
import com.example.backend.exception.NotFoundException;
import com.example.backend.repository.DepartmentRepository;
import com.example.backend.repository.MentorRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

import com.example.backend.dto.response.InternProfileResponse;
import com.example.backend.entity.InternProfile;
import org.springframework.data.jpa.domain.Specification;

@Service
@RequiredArgsConstructor
@Slf4j
public class MentorService {

        private final MentorRepository mentorRepository;
        private final UserRepository userRepository;
        private final DepartmentRepository departmentRepository;
        private final com.example.backend.repository.TaskRepository taskRepository;
        private final com.example.backend.repository.WeeklyReportRepository weeklyReportRepository;
        private final com.example.backend.repository.InternProfileRepository internProfileRepository;
        private final com.example.backend.repository.GroupMemberRepository groupMemberRepository;

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
                                        .orElseThrow(() -> new NotFoundException("Department",
                                                        request.getDepartmentId()));
                }

                Mentor mentor = new Mentor();
                mentor.setUser(user);
                mentor.setDepartment(department);
                mentor.setTitle(request.getTitle());

                mentor = mentorRepository.save(mentor);
                log.info("Created mentor profile for user: {}", user.getEmail());

                return mapToResponse(mentor);
        }

        @Transactional(readOnly = true)
        public List<MentorResponse> getAllMentors() {
                return mentorRepository.findAllWithUserExcludingInterns().stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public org.springframework.data.domain.Page<MentorResponse> getMentors(
                        org.springframework.data.domain.Pageable pageable) {
                return mentorRepository.findMentorsExcludingInterns(pageable)
                                .map(this::mapToResponse);
        }

        @Transactional(readOnly = true)
        public MentorResponse getMentorById(Long id) {
                Mentor mentor = mentorRepository.findById(id)
                                .orElseThrow(() -> new NotFoundException("Mentor", id));
                return mapToResponse(mentor);
        }

        @Transactional(readOnly = true)
        public MentorResponse getMentorByUserId(Long userId) {
                Mentor mentor = mentorRepository.findByUser_Id(userId)
                                .orElseThrow(() -> new NotFoundException("Mentor not found for user: " + userId));
                return mapToResponse(mentor);
        }

        @Transactional(readOnly = true)
        public com.example.backend.dto.response.MentorDashboardStats getDashboardStats(String email) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new NotFoundException("User not found: " + email));

                Mentor mentor = mentorRepository.findByUser_Id(user.getId())
                                .orElseThrow(() -> new NotFoundException(
                                                "Mentor profile not found for user: " + email));

                long totalInterns = internProfileRepository.countByMentorId(mentor.getId());
                long activeTasks = taskRepository.countByCreatedByIdAndStatusNot(user.getId(),
                                com.example.backend.enums.TaskStatus.DONE);
                long pendingReports = weeklyReportRepository.countByMentor_IdAndStatus(user.getId(),
                                com.example.backend.enums.WeeklyReportStatus.SUBMITTED.name());

                List<com.example.backend.dto.response.TaskResponse> recentTasks = taskRepository
                                .findTop5ByCreatedByIdOrderByCreatedAtDesc(user.getId())
                                .stream()
                                .map(this::mapToTaskResponse)
                                .collect(Collectors.toList());

                List<com.example.backend.dto.WeeklyReportDto> recentReports = weeklyReportRepository
                                .findTop5ByMentor_IdOrderByWeekNumberDesc(user.getId())
                                .stream()
                                .map(this::mapToReportDto)
                                .collect(Collectors.toList());

                return com.example.backend.dto.response.MentorDashboardStats.builder()
                                .totalInterns(totalInterns)
                                .activeTasks(activeTasks)
                                .pendingReports(pendingReports)
                                .recentTasks(recentTasks)
                                .recentReports(recentReports)
                                .build();
        }

        @Transactional(readOnly = true)
        public org.springframework.data.domain.Page<InternProfileResponse> getAssignedInterns(
                        String email, String keyword, String status,
                        org.springframework.data.domain.Pageable pageable) {
                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new NotFoundException("User not found: " + email));

                Mentor mentor = mentorRepository.findByUser_Id(user.getId())
                                .orElseThrow(() -> new NotFoundException(
                                                "Mentor profile not found for user: " + email));

                Specification<InternProfile> spec = (root, query, cb) -> {
                        List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

                        // 1. Keyword search (moved up to be common)
                        if (keyword != null && !keyword.trim().isEmpty()) {
                                String likePattern = "%" + keyword.toLowerCase() + "%";
                                predicates.add(cb.or(
                                                cb.like(cb.lower(root.get("user").get("fullName")), likePattern),
                                                cb.like(cb.lower(root.get("user").get("email")), likePattern),
                                                cb.like(cb.lower(root.get("studentCode")), likePattern)));
                        }

                        // 2. Mentor assignment (Strictly via Group)
                        jakarta.persistence.criteria.Subquery<Long> subquery = query.subquery(Long.class);
                        jakarta.persistence.criteria.Root<com.example.backend.entity.GroupMember> gmRoot = subquery
                                        .from(com.example.backend.entity.GroupMember.class);
                        subquery.select(gmRoot.get("intern").get("id"));

                        // Condition: Group belonged to mentor AND member is active (leftAt is null)
                        subquery.where(
                                        cb.equal(gmRoot.get("group").get("mentorId"), mentor.getId()),
                                        cb.isNull(gmRoot.get("leftAt")));

                        // Main Condition: ONLY In a group managed by mentor
                        // Removed directAssign (cb.equal(root.get("mentor").get("id"), mentor.getId()))
                        jakarta.persistence.criteria.Predicate groupAssign = root.get("id").in(subquery);

                        predicates.add(groupAssign);

                        // Status filter (Optional - currently ignored as no status field, or implement
                        // logic if field exists)
                        // if (status != null && !status.isEmpty()) { ... }

                        return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
                };

                return internProfileRepository.findAll(spec, pageable)
                                .map(this::mapToInternProfileResponse);
        }

        @Transactional(readOnly = true)
        public com.example.backend.dto.response.InternProfileResponse getInternDetail(Long internId) {
                com.example.backend.entity.InternProfile profile = internProfileRepository.findById(internId)
                                .orElseThrow(() -> new NotFoundException("Intern profile", internId));
                return mapToInternProfileResponse(profile);
        }

        private com.example.backend.dto.response.TaskResponse mapToTaskResponse(com.example.backend.entity.Task task) {
                return com.example.backend.dto.response.TaskResponse.builder()
                                .id(task.getId())
                                .title(task.getTitle())
                                .description(task.getDescription())
                                .status(task.getStatus())
                                .dueDate(task.getDueDate())
                                .createdAt(task.getCreatedAt())
                                .groupName(task.getGroup() != null ? task.getGroup().getName() : null)
                                .build();
        }

        private com.example.backend.dto.WeeklyReportDto mapToReportDto(com.example.backend.entity.WeeklyReport report) {
                return new com.example.backend.dto.WeeklyReportDto(
                                report.getId(),
                                report.getIntern().getId(),
                                report.getIntern().getUser().getFullName(),
                                report.getWeekNumber(),
                                report.getWeekStart(),
                                report.getWeekEnd(),
                                report.getReportDate(),
                                report.getCompletedWork(),
                                report.getPlannedWork(),
                                report.getChallenges(),
                                report.getLearnings(),
                                report.getLearnings(), // summary alias
                                com.example.backend.enums.WeeklyReportStatus.valueOf(report.getStatus()),
                                report.getMentorFeedback(),
                                report.getRating(),
                                report.getMentor() != null ? report.getMentor().getId() : null,
                                report.getMentor() != null ? report.getMentor().getFullName() : null,
                                report.getCreatedAt(),
                                report.getReviewedAt());
        }

        private InternProfileResponse mapToInternProfileResponse(InternProfile profile) {
                var responseBuilder = InternProfileResponse.builder()
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
                                .mentorName(profile.getMentor() != null ? profile.getMentor().getUser().getFullName()
                                                : null)
                                .createdAt(profile.getCreatedAt())
                                .updatedAt(profile.getUpdatedAt());

                // Fetch active group
                com.example.backend.entity.GroupMember member = groupMemberRepository
                                .findFirstByIntern_IdAndLeftAtIsNull(profile.getId()).orElse(null);

                if (member != null && member.getGroup() != null) {
                        responseBuilder.programGroupId(member.getGroup().getId())
                                        .programGroupName(member.getGroup().getName());

                        if (member.getGroup().getProgram() != null) {
                                // Sync dates from Program
                                responseBuilder.startDate(member.getGroup().getProgram().getStartDate());
                                responseBuilder.endDate(member.getGroup().getProgram().getEndDate());

                                // Sync status from Program/Group logic
                                // If Member is active (leftAt == null) and Program is ACTIVE -> ACTIVE
                                // If Program CLOSED -> FINISHED
                                if ("CLOSED".equals(member.getGroup().getProgram().getStatus().name())) {
                                        responseBuilder.status("FINISHED");
                                } else {
                                        responseBuilder.status("ACTIVE");
                                }
                        } else {
                                responseBuilder.status("ACTIVE"); // Default if in group
                        }
                } else {
                        // Not in any group -> UNASSIGNED or FINISHED based on dates?
                        // For now, let's look at profile dates
                        java.time.LocalDate now = java.time.LocalDate.now();
                        if (profile.getEndDate() != null && profile.getEndDate().isBefore(now)) {
                                responseBuilder.status("FINISHED");
                        } else if (profile.getStartDate() != null && profile.getStartDate().isAfter(now)) {
                                responseBuilder.status("WAITING");
                        } else {
                                responseBuilder.status("ACTIVE");
                        }
                }

                return responseBuilder.build();
        }

        @Transactional
        public MentorResponse updateMentor(Long id, CreateMentorRequest request) {
                Mentor mentor = mentorRepository.findById(id)
                                .orElseThrow(() -> new NotFoundException("Mentor", id));

                // Update department if provided
                if (request.getDepartmentId() != null) {
                        Department department = departmentRepository.findById(request.getDepartmentId())
                                        .orElseThrow(() -> new NotFoundException("Department",
                                                        request.getDepartmentId()));
                        mentor.setDepartment(department);
                }

                // Update title
                mentor.setTitle(request.getTitle());

                mentor = mentorRepository.save(mentor);
                log.info("Updated mentor: {}", mentor.getId());

                return mapToResponse(mentor);
        }

        private MentorResponse mapToResponse(Mentor mentor) {
                MentorResponse.MentorResponseBuilder builder = MentorResponse.builder()
                                .id(mentor.getId())
                                .userId(mentor.getUser().getId())
                                .email(mentor.getUser().getEmail())
                                .fullName(mentor.getUser().getFullName())
                                .title(mentor.getTitle())
                                .createdAt(mentor.getCreatedAt());

                // Add department info if exists
                if (mentor.getDepartment() != null) {
                        builder.departmentId(mentor.getDepartment().getId())
                                        .departmentName(mentor.getDepartment().getName());
                }

                // Add intern count
                builder.internCount(internProfileRepository.countByMentorId(mentor.getId()));

                return builder.build();
        }
}
