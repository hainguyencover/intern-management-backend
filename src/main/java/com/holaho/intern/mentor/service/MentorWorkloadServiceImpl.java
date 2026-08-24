package com.holaho.intern.mentor.service;

import com.holaho.intern.mentor.dto.*;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.mentor.entity.MentorAssignment;
import com.holaho.intern.mentor.repository.MentorAssignmentRepository;
import com.holaho.intern.mentor.repository.MentorRepository;
import com.holaho.intern.shared.config.TenantContext;
import com.holaho.intern.shared.enums.MentorStatus;
import com.holaho.intern.shared.enums.MentorWorkloadStatus;
import com.holaho.intern.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MentorWorkloadServiceImpl implements MentorWorkloadService {

    private final MentorRepository mentorRepository;
    private final MentorAssignmentRepository assignmentRepository;

    private Long getCurrentTenantId() {
        Long tenantId = TenantContext.getCurrentTenantId();
        return tenantId != null ? tenantId : 1L;
    }

    @Override
    public Page<MentorWorkloadResponse> getWorkloads(
            String keyword,
            Long departmentId,
            MentorWorkloadStatus workloadStatus,
            Pageable pageable
    ) {
        Long tenantId = getCurrentTenantId();

        // 1. Load active mentors matching keyword and department
        Page<Mentor> mentorPage = mentorRepository.searchMentors(
                tenantId,
                keyword,
                MentorStatus.ACTIVE,
                departmentId,
                pageable
        );

        // 2. Load active workload aggregation map for tenant
        Map<Long, Long> workloadMap = getActiveWorkloadMap(tenantId);

        // 3. Map to DTOs
        List<MentorWorkloadResponse> workloadResponses = mentorPage.getContent().stream()
                .map(mentor -> mapToResponse(mentor, workloadMap))
                .filter(res -> workloadStatus == null || res.getWorkloadStatus() == workloadStatus)
                .collect(Collectors.toList());

        return new PageImpl<>(workloadResponses, pageable, mentorPage.getTotalElements());
    }

    @Override
    public MentorWorkloadSummaryResponse getWorkloadSummary() {
        Long tenantId = getCurrentTenantId();
        List<Mentor> mentors = mentorRepository.findAllByTenantIdWithDetails(tenantId);
        Map<Long, Long> workloadMap = getActiveWorkloadMap(tenantId);

        long totalMentors = mentors.size();
        long mentorsWithInterns = 0;
        long totalActiveInterns = 0;

        long noAssignmentCount = 0;
        long underloadCount = 0;
        long normalCount = 0;
        long nearCapacityCount = 0;
        long overloadCount = 0;

        for (Mentor mentor : mentors) {
            int currentCount = workloadMap.getOrDefault(mentor.getId(), 0L).intValue();
            int capacity = mentor.getCapacity() > 0 ? mentor.getCapacity() : 10;
            MentorWorkloadStatus status = calculateWorkloadStatus(currentCount, capacity);

            if (currentCount > 0) {
                mentorsWithInterns++;
                totalActiveInterns += currentCount;
            }

            switch (status) {
                case NO_ASSIGNMENT -> noAssignmentCount++;
                case UNDERLOAD -> underloadCount++;
                case NORMAL -> normalCount++;
                case NEAR_CAPACITY -> nearCapacityCount++;
                case OVERLOAD -> overloadCount++;
            }
        }

        double averageRatio = totalMentors > 0
                ? Math.round(((double) totalActiveInterns / totalMentors) * 100.0) / 100.0
                : 0.0;

        return MentorWorkloadSummaryResponse.builder()
                .totalMentors(totalMentors)
                .mentorsWithInterns(mentorsWithInterns)
                .totalActiveInterns(totalActiveInterns)
                .averageInternsPerMentor(averageRatio)
                .noAssignmentCount(noAssignmentCount)
                .underloadCount(underloadCount)
                .normalCount(normalCount)
                .nearCapacityCount(nearCapacityCount)
                .overloadCount(overloadCount)
                .build();
    }

    @Override
    public List<MentorActiveInternResponse> getMentorActiveInterns(Long mentorId) {
        Long tenantId = getCurrentTenantId();

        // Verify mentor exists in tenant
        mentorRepository.findByTenantIdAndId(tenantId, mentorId)
                .orElseThrow(() -> new ResourceNotFoundException("Mentor not found with ID: " + mentorId));

        List<MentorAssignment> assignments = assignmentRepository.findActiveAssignmentsWithDetailsByMentorId(tenantId, mentorId);

        return assignments.stream()
                .map(this::mapToActiveInternResponse)
                .collect(Collectors.toList());
    }

    @Override
    public MentorWorkloadStatus calculateWorkloadStatus(int currentCount, int maxCapacity) {
        if (currentCount == 0) {
            return MentorWorkloadStatus.NO_ASSIGNMENT;
        }

        if (maxCapacity <= 0) {
            return MentorWorkloadStatus.OVERLOAD;
        }

        int utilization = (currentCount * 100) / maxCapacity;

        if (utilization > 100) {
            return MentorWorkloadStatus.OVERLOAD;
        }
        if (utilization >= 81) {
            return MentorWorkloadStatus.NEAR_CAPACITY;
        }
        if (utilization >= 61) {
            return MentorWorkloadStatus.NORMAL;
        }
        return MentorWorkloadStatus.UNDERLOAD;
    }

    private Map<Long, Long> getActiveWorkloadMap(Long tenantId) {
        return assignmentRepository.countActiveWorkloadsByTenantId(tenantId).stream()
                .collect(Collectors.toMap(
                        MentorWorkloadProjection::getMentorId,
                        MentorWorkloadProjection::getActiveInternCount
                ));
    }

    private MentorWorkloadResponse mapToResponse(Mentor mentor, Map<Long, Long> workloadMap) {
        int currentCount = workloadMap.getOrDefault(mentor.getId(), 0L).intValue();
        int capacity = mentor.getCapacity() > 0 ? mentor.getCapacity() : 10;
        int utilizationPercent = capacity > 0 ? (currentCount * 100) / capacity : 100;
        MentorWorkloadStatus status = calculateWorkloadStatus(currentCount, capacity);

        String deptName = mentor.getDepartment() != null ? mentor.getDepartment().getName() : null;
        Long deptId = mentor.getDepartment() != null ? mentor.getDepartment().getId() : null;
        String email = mentor.getUser() != null ? mentor.getUser().getEmail() : null;

        return MentorWorkloadResponse.builder()
                .mentorId(mentor.getId())
                .userId(mentor.getUser() != null ? mentor.getUser().getId() : null)
                .employeeCode(mentor.getEmployeeCode())
                .mentorName(mentor.getFullName())
                .email(email)
                .phone(mentor.getPhone())
                .departmentId(deptId)
                .departmentName(deptName)
                .position(mentor.getPosition())
                .specialization(mentor.getSpecialization())
                .currentInternCount(currentCount)
                .maxInternCapacity(capacity)
                .utilizationPercent(utilizationPercent)
                .workloadStatus(status)
                .lastUpdatedAt(mentor.getUpdatedAt())
                .build();
    }

    private MentorActiveInternResponse mapToActiveInternResponse(MentorAssignment assignment) {
        var intern = assignment.getIntern();
        var user = intern != null ? intern.getUser() : null;

        return MentorActiveInternResponse.builder()
                .assignmentId(assignment.getId())
                .internId(intern != null ? intern.getId() : null)
                .internCode(intern != null ? intern.getInternCode() : null)
                .fullName(user != null ? user.getFullName() : (intern != null ? intern.getInternCode() : "N/A"))
                .email(user != null ? user.getEmail() : null)
                .phone(user != null ? user.getPhone() : null)
                .university(intern != null ? intern.getUniversity() : null)
                .programName(intern != null && intern.getProgram() != null ? intern.getProgram().getName() : null)
                .assignmentType(assignment.getAssignmentType() != null ? assignment.getAssignmentType().name() : "PRIMARY")
                .status(assignment.getStatus() != null ? assignment.getStatus().name() : "ACTIVE")
                .startDate(assignment.getStartDate())
                .endDate(assignment.getEndDate())
                .assignedAt(assignment.getAssignedAt())
                .note(assignment.getNote())
                .build();
    }
}
