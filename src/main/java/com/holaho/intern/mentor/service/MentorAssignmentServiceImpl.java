package com.holaho.intern.mentor.service;

import com.holaho.intern.entity.InternshipEnrollment;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.mentor.dto.*;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.mentor.entity.MentorAssignment;
import com.holaho.intern.mentor.exception.InvalidMentorAssignmentException;
import com.holaho.intern.mentor.exception.MentorNotFoundException;
import com.holaho.intern.mentor.repository.MentorAssignmentRepository;
import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.mentor.validation.MentorAssignmentValidator;
import com.holaho.intern.repository.InternshipEnrollmentRepository;
import com.holaho.intern.shared.config.TenantContext;
import com.holaho.intern.shared.dto.response.AssignMentorResponse;
import com.holaho.intern.shared.dto.response.MentorAssignmentResponse;
import com.holaho.intern.shared.dto.response.MentorCapacityResponse;
import com.holaho.intern.shared.dto.response.MentorResponse;
import com.holaho.intern.shared.enums.MentorAssignmentStatus;
import com.holaho.intern.shared.enums.MentorAssignmentType;
import com.holaho.intern.shared.enums.MentorStatus;
import com.holaho.intern.shared.enums.UserStatus;
import com.holaho.intern.shared.exception.ApiException;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MentorAssignmentServiceImpl implements MentorAssignmentService {

    private final InternProfileRepository internProfileRepository;
    private final MentorRepository mentorRepository;
    private final MentorAssignmentRepository mentorAssignmentRepository;
    private final InternshipEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    private final MentorAssignmentValidator assignmentValidator;
    private final com.holaho.intern.user.service.CurrentUserService currentUserService;

    private Long getCurrentTenantId() {
        Long tenantId = TenantContext.getCurrentTenantId();
        return tenantId != null ? tenantId : 1L;
    }

    @Override
    @Transactional
    public MentorAssignmentResponse assignMentor(AssignMentorRequest request) {
        Long tenantId = getCurrentTenantId();

        Mentor mentor = mentorRepository.findByTenantIdAndId(tenantId, request.mentorId())
                .orElseThrow(() -> new MentorNotFoundException(request.mentorId()));

        InternProfile intern = internProfileRepository.findById(request.internId())
                .orElseThrow(() -> new NotFoundException("Intern profile", request.internId()));

        // Business Rule Validations
        assignmentValidator.validateTenantMatch(mentor, intern);
        assignmentValidator.validateMentorActive(mentor);
        assignmentValidator.validateMentorCapacity(mentor, tenantId);

        MentorAssignmentType type = request.assignmentType() != null ? request.assignmentType() : MentorAssignmentType.PRIMARY;
        if (type == MentorAssignmentType.PRIMARY) {
            assignmentValidator.validatePrimaryAssignmentOverlap(tenantId, intern.getId(), request.startDate(), request.endDate(), null);
        }

        InternshipEnrollment enrollment = null;
        if (request.enrollmentId() != null) {
            enrollment = enrollmentRepository.findById(request.enrollmentId()).orElse(null);
        } else {
            enrollment = enrollmentRepository.findActiveEnrollmentByInternId(intern.getId()).orElse(null);
        }

        LocalDateTime now = LocalDateTime.now();
        MentorAssignment assignment = MentorAssignment.builder()
                .enrollment(enrollment)
                .intern(intern)
                .mentor(mentor)
                .assignmentType(type)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .assignedAt(now)
                .status(MentorAssignmentStatus.ACTIVE)
                .responsibility(request.responsibility())
                .build();

        assignment.setTenantId(tenantId);
        assignment = mentorAssignmentRepository.save(assignment);

        if (type == MentorAssignmentType.PRIMARY) {
            intern.setMentor(mentor);
            internProfileRepository.save(intern);
        }

        log.info("Assigned mentor ID {} to intern ID {}. Assignment ID: {}", mentor.getId(), intern.getId(), assignment.getId());
        return mapToAssignmentResponse(assignment);
    }

    @Override
    @Transactional
    public MentorAssignmentResponse reassignMentor(Long assignmentId, ReassignMentorRequest request) {
        Long tenantId = getCurrentTenantId();

        MentorAssignment oldAssignment = mentorAssignmentRepository.findByTenantIdAndId(tenantId, assignmentId)
                .orElseThrow(() -> new NotFoundException("Mentor assignment", assignmentId));

        Mentor newMentor = mentorRepository.findByTenantIdAndId(tenantId, request.newMentorId())
                .orElseThrow(() -> new MentorNotFoundException(request.newMentorId()));

        InternProfile intern = oldAssignment.getIntern();

        // Validate new mentor
        assignmentValidator.validateTenantMatch(newMentor, intern);
        assignmentValidator.validateMentorActive(newMentor);
        assignmentValidator.validateMentorCapacity(newMentor, tenantId);

        LocalDateTime now = LocalDateTime.now();
        // Close old assignment
        oldAssignment.setStatus(MentorAssignmentStatus.COMPLETED);
        oldAssignment.setEndedAt(now);
        if (request.effectiveStartDate() != null) {
            oldAssignment.setEndDate(request.effectiveStartDate().minusDays(1));
        }
        oldAssignment.setReason(request.reason());
        mentorAssignmentRepository.save(oldAssignment);

        MentorAssignmentType type = request.assignmentType() != null ? request.assignmentType() : oldAssignment.getAssignmentType();

        // Create new assignment
        MentorAssignment newAssignment = MentorAssignment.builder()
                .enrollment(oldAssignment.getEnrollment())
                .intern(intern)
                .mentor(newMentor)
                .assignmentType(type)
                .startDate(request.effectiveStartDate() != null ? request.effectiveStartDate() : LocalDate.now())
                .endDate(request.endDate())
                .assignedAt(now)
                .status(MentorAssignmentStatus.ACTIVE)
                .responsibility(request.responsibility() != null ? request.responsibility() : oldAssignment.getResponsibility())
                .reason("Reassigned from mentor ID: " + oldAssignment.getMentor().getId() + (request.reason() != null ? " - " + request.reason() : ""))
                .build();

        newAssignment.setTenantId(tenantId);
        newAssignment = mentorAssignmentRepository.save(newAssignment);

        if (type == MentorAssignmentType.PRIMARY) {
            intern.setMentor(newMentor);
            internProfileRepository.save(intern);
        }

        log.info("Reassigned intern ID {} from mentor ID {} to mentor ID {}. New assignment ID: {}",
                intern.getId(), oldAssignment.getMentor().getId(), newMentor.getId(), newAssignment.getId());

        return mapToAssignmentResponse(newAssignment);
    }

    @Override
    @Transactional
    public MentorAssignmentResponse unassignMentor(Long assignmentId, UnassignMentorRequest request) {
        Long tenantId = getCurrentTenantId();

        MentorAssignment assignment = mentorAssignmentRepository.findByTenantIdAndId(tenantId, assignmentId)
                .orElseThrow(() -> new NotFoundException("Mentor assignment", assignmentId));

        if (assignment.getStatus() != MentorAssignmentStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "Mentor assignment is not active (current status: " + assignment.getStatus() + ")");
        }

        LocalDateTime now = LocalDateTime.now();
        assignment.setStatus(MentorAssignmentStatus.UNASSIGNED);
        assignment.setEndedAt(now);
        assignment.setUnassignedAt(now);

        try {
            User currentUser = currentUserService.getCurrentUserEntity();
            assignment.setUnassignedBy(currentUser);
        } catch (Exception e) {
            log.debug("Current user entity not found when unassigning mentor: {}", e.getMessage());
        }

        String reason = request != null && request.reason() != null ? request.reason().trim() : "Unassigned mentor";
        assignment.setUnassignReason(reason);
        assignment.setReason(reason);

        assignment = mentorAssignmentRepository.save(assignment);

        InternProfile intern = assignment.getIntern();
        if (intern != null && intern.getMentor() != null && intern.getMentor().getId().equals(assignment.getMentor().getId())) {
            intern.setMentor(null);
            internProfileRepository.save(intern);
        }

        log.info("Unassigned mentor ID {} from intern ID {}. Assignment ID: {}",
                assignment.getMentor().getId(), intern != null ? intern.getId() : null, assignmentId);

        return mapToAssignmentResponse(assignment);
    }

    @Override
    @Transactional
    public MentorAssignmentResponse completeAssignment(Long assignmentId, String reason) {
        Long tenantId = getCurrentTenantId();

        MentorAssignment assignment = mentorAssignmentRepository.findByTenantIdAndId(tenantId, assignmentId)
                .orElseThrow(() -> new NotFoundException("Mentor assignment", assignmentId));

        LocalDateTime now = LocalDateTime.now();
        assignment.setStatus(MentorAssignmentStatus.COMPLETED);
        assignment.setEndedAt(now);
        if (assignment.getEndDate() == null) {
            assignment.setEndDate(LocalDate.now());
        }
        if (reason != null) {
            assignment.setReason(reason);
        }
        assignment = mentorAssignmentRepository.save(assignment);

        InternProfile intern = assignment.getIntern();
        if (intern != null && intern.getMentor() != null && intern.getMentor().getId().equals(assignment.getMentor().getId())) {
            intern.setMentor(null);
            internProfileRepository.save(intern);
        }

        log.info("Completed mentor assignment ID {}", assignmentId);
        return mapToAssignmentResponse(assignment);
    }

    @Override
    @Transactional
    public MentorAssignmentResponse cancelAssignment(Long assignmentId, String reason) {
        Long tenantId = getCurrentTenantId();

        MentorAssignment assignment = mentorAssignmentRepository.findByTenantIdAndId(tenantId, assignmentId)
                .orElseThrow(() -> new NotFoundException("Mentor assignment", assignmentId));

        LocalDateTime now = LocalDateTime.now();
        assignment.setStatus(MentorAssignmentStatus.CANCELLED);
        assignment.setEndedAt(now);
        if (reason != null) {
            assignment.setReason(reason);
        }
        assignment = mentorAssignmentRepository.save(assignment);

        InternProfile intern = assignment.getIntern();
        if (intern != null && intern.getMentor() != null && intern.getMentor().getId().equals(assignment.getMentor().getId())) {
            intern.setMentor(null);
            internProfileRepository.save(intern);
        }

        log.info("Cancelled mentor assignment ID {}", assignmentId);
        return mapToAssignmentResponse(assignment);
    }

    @Override
    @Transactional
    public BulkAssignMentorResponse bulkAssignMentors(BulkAssignMentorRequest request) {
        List<BulkAssignMentorResponse.BulkAssignItemResult> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (Long internId : request.internIds()) {
            try {
                AssignMentorRequest singleReq = AssignMentorRequest.builder()
                        .mentorId(request.mentorId())
                        .internId(internId)
                        .assignmentType(request.assignmentType())
                        .startDate(request.startDate())
                        .endDate(request.endDate())
                        .responsibility(request.responsibility())
                        .build();

                MentorAssignmentResponse res = assignMentor(singleReq);
                results.add(BulkAssignMentorResponse.BulkAssignItemResult.builder()
                        .internId(internId)
                        .success(true)
                        .assignmentId(res.getId())
                        .build());
                successCount++;
            } catch (Exception e) {
                log.warn("Bulk assignment failed for intern ID {}: {}", internId, e.getMessage());
                results.add(BulkAssignMentorResponse.BulkAssignItemResult.builder()
                        .internId(internId)
                        .success(false)
                        .errorCode(e.getClass().getSimpleName())
                        .errorMessage(e.getMessage())
                        .build());
                failureCount++;
            }
        }

        return BulkAssignMentorResponse.builder()
                .totalRequested(request.internIds().size())
                .successCount(successCount)
                .failureCount(failureCount)
                .results(results)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public MentorSuggestionResponse suggestMentorsForIntern(Long internId) {
        Long tenantId = getCurrentTenantId();

        InternProfile intern = internProfileRepository.findById(internId)
                .orElseThrow(() -> new NotFoundException("Intern profile", internId));

        List<Mentor> mentors = mentorRepository.findAllByTenantIdWithDetails(tenantId).stream()
                .filter(m -> m.getStatus() == MentorStatus.ACTIVE)
                .toList();

        List<MentorSuggestionResponse.SuggestedMentorItem> items = new ArrayList<>();

        for (Mentor mentor : mentors) {
            long activeCount = mentorAssignmentRepository.countByTenantIdAndMentorIdAndStatus(
                    tenantId, mentor.getId(), MentorAssignmentStatus.ACTIVE
            );
            int capacity = mentor.getCapacity() != null ? mentor.getCapacity() : 5;
            boolean hasCapacity = activeCount < capacity;

            if (!hasCapacity) {
                continue;
            }

            int score = 0;
            StringBuilder reasonBuilder = new StringBuilder();

            boolean sameDept = mentor.getDepartment() != null && intern.getDepartment() != null
                    && mentor.getDepartment().getId().equals(intern.getDepartment().getId());

            if (sameDept) {
                score += 50;
                reasonBuilder.append("Cùng phòng ban (").append(mentor.getDepartment().getName()).append("); ");
            }

            score += 30; // Has capacity slot
            reasonBuilder.append("Còn slot (").append(activeCount).append("/").append(capacity).append("); ");

            if (mentor.getYearsOfExperience() != null && mentor.getYearsOfExperience().compareTo(BigDecimal.valueOf(3)) >= 0) {
                score += 20;
                reasonBuilder.append("Kinh nghiệm lâu năm (").append(mentor.getYearsOfExperience()).append(" năm); ");
            }

            MentorResponse mResp = mapToMentorResponse(mentor, (int) activeCount);

            items.add(MentorSuggestionResponse.SuggestedMentorItem.builder()
                    .mentor(mResp)
                    .matchScorePercentage(Math.min(100, score))
                    .matchReason(reasonBuilder.toString().trim())
                    .sameDepartmentMatch(sameDept)
                    .currentAssignedCount((int) activeCount)
                    .maxCapacity(capacity)
                    .build());
        }

        items.sort((a, b) -> Integer.compare(b.matchScorePercentage(), a.matchScorePercentage()));

        return MentorSuggestionResponse.builder()
                .internId(intern.getId())
                .internName(intern.getUser() != null ? intern.getUser().getFullName() : "")
                .internDepartmentName(intern.getDepartment() != null ? intern.getDepartment().getName() : "")
                .suggestions(items)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentorAssignmentResponse> getInternAssignmentHistory(Long internId) {
        Long tenantId = getCurrentTenantId();
        return mentorAssignmentRepository.findByTenantIdAndInternIdOrderByStartDateDescAssignedAtDesc(tenantId, internId).stream()
                .map(this::mapToAssignmentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentorAssignmentResponse> getMentorActiveInterns(Long mentorId) {
        Long tenantId = getCurrentTenantId();
        return mentorAssignmentRepository.findActiveAssignmentsWithDetailsByMentorId(tenantId, mentorId).stream()
                .map(this::mapToAssignmentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MentorCapacityResponse getMentorCapacity(Long mentorId) {
        Long tenantId = getCurrentTenantId();
        Mentor mentor = mentorRepository.findByTenantIdAndId(tenantId, mentorId)
                .orElseThrow(() -> new MentorNotFoundException(mentorId));

        long count = mentorAssignmentRepository.countByTenantIdAndMentorIdAndStatus(tenantId, mentorId, MentorAssignmentStatus.ACTIVE);
        int maxCap = mentor.getCapacity() != null ? mentor.getCapacity() : 5;
        boolean isFull = count >= maxCap;
        double pct = maxCap > 0 ? ((double) count / maxCap) * 100.0 : 0.0;

        return MentorCapacityResponse.builder()
                .mentorId(mentor.getId())
                .mentorName(mentor.getFullName())
                .mentorEmail(mentor.getUser() != null ? mentor.getUser().getEmail() : null)
                .departmentId(mentor.getDepartment() != null ? mentor.getDepartment().getId() : null)
                .departmentName(mentor.getDepartment() != null ? mentor.getDepartment().getName() : null)
                .activeInternCount(count)
                .maxCapacity(maxCap)
                .full(isFull)
                .capacityPercentage(pct)
                .build();
    }

    // Legacy fallback methods
    @Override
    @Transactional
    public AssignMentorResponse assignMentorToIntern(Long internId, Long mentorId) {
        return assignMentorToIntern(internId, mentorId, null);
    }

    @Override
    @Transactional
    public AssignMentorResponse assignMentorToIntern(Long internId, Long mentorId, String reason) {
        MentorAssignmentResponse resp = assignMentor(AssignMentorRequest.builder()
                .mentorId(mentorId)
                .internId(internId)
                .startDate(LocalDate.now())
                .responsibility(reason)
                .build());

        AssignMentorResponse amr = new AssignMentorResponse();
        amr.setInternId(internId);
        amr.setMentorId(mentorId);
        amr.setAssignedAt(resp.getAssignedAt());
        return amr;
    }

    @Override
    @Transactional
    public AssignMentorResponse removeMentorFromIntern(Long internId) {
        Long tenantId = getCurrentTenantId();
        Optional<MentorAssignment> active = mentorAssignmentRepository.findByTenantIdAndInternIdAndStatus(
                tenantId, internId, MentorAssignmentStatus.ACTIVE
        );

        if (active.isPresent()) {
            completeAssignment(active.get().getId(), "Removed mentor");
        }

        AssignMentorResponse amr = new AssignMentorResponse();
        amr.setInternId(internId);
        amr.setAssignedAt(LocalDateTime.now());
        return amr;
    }

    private MentorAssignmentResponse mapToAssignmentResponse(MentorAssignment ma) {
        return MentorAssignmentResponse.builder()
                .id(ma.getId())
                .enrollmentId(ma.getEnrollment() != null ? ma.getEnrollment().getId() : null)
                .internId(ma.getIntern().getId())
                .internName(ma.getIntern().getUser() != null ? ma.getIntern().getUser().getFullName() : "")
                .internEmail(ma.getIntern().getUser() != null ? ma.getIntern().getUser().getEmail() : "")
                .internCode(ma.getIntern().getStudentCode())
                .mentorId(ma.getMentor().getId())
                .mentorName(ma.getMentor().getFullName())
                .mentorEmail(ma.getMentor().getUser() != null ? ma.getMentor().getUser().getEmail() : "")
                .mentorEmployeeCode(ma.getMentor().getEmployeeCode())
                .mentorDepartmentId(ma.getMentor().getDepartment() != null ? ma.getMentor().getDepartment().getId() : null)
                .mentorDepartmentName(ma.getMentor().getDepartment() != null ? ma.getMentor().getDepartment().getName() : null)
                .assignmentType(ma.getAssignmentType())
                .startDate(ma.getStartDate())
                .endDate(ma.getEndDate())
                .status(ma.getStatus())
                .responsibility(ma.getResponsibility())
                .assignedAt(ma.getAssignedAt())
                .endedAt(ma.getEndedAt())
                .assignedById(ma.getAssignedBy() != null ? ma.getAssignedBy().getId() : null)
                .assignedByName(ma.getAssignedBy() != null ? ma.getAssignedBy().getFullName() : null)
                .unassignedAt(ma.getUnassignedAt())
                .unassignedBy(ma.getUnassignedBy() != null ? ma.getUnassignedBy().getId() : null)
                .unassignedByName(ma.getUnassignedBy() != null ? ma.getUnassignedBy().getFullName() : null)
                .unassignReason(ma.getUnassignReason())
                .reason(ma.getReason())
                .build();
    }

    private MentorResponse mapToMentorResponse(Mentor mentor, int activeCount) {
        int capacity = mentor.getCapacity() != null ? mentor.getCapacity() : 5;
        int available = Math.max(0, capacity - activeCount);
        double utilization = capacity > 0 ? ((double) activeCount / capacity) * 100.0 : 0.0;

        return MentorResponse.builder()
                .id(mentor.getId())
                .userId(mentor.getUser() != null ? mentor.getUser().getId() : null)
                .email(mentor.getUser() != null ? mentor.getUser().getEmail() : null)
                .fullName(mentor.getFullName())
                .phone(mentor.getPhone())
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
}
