package com.holaho.intern.mentor.validation;

import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.mentor.entity.Mentor;
import com.holaho.intern.mentor.entity.MentorAssignment;
import com.holaho.intern.mentor.exception.InvalidMentorAssignmentException;
import com.holaho.intern.mentor.exception.MentorCapacityExceededException;
import com.holaho.intern.mentor.repository.MentorAssignmentRepository;
import com.holaho.intern.shared.enums.MentorAssignmentStatus;
import com.holaho.intern.shared.enums.MentorAssignmentType;
import com.holaho.intern.shared.enums.MentorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class MentorAssignmentValidator {

    private final MentorAssignmentRepository mentorAssignmentRepository;

    /**
     * Validates tenant isolation rule BR-MEN-01
     */
    public void validateTenantMatch(Mentor mentor, InternProfile intern) {
        if (!Objects.equals(mentor.getTenantId(), intern.getTenantId())) {
            throw new InvalidMentorAssignmentException(
                    String.format("Cross-tenant assignment rejected. Mentor tenant (%d) does not match Intern tenant (%d)",
                            mentor.getTenantId(), intern.getTenantId())
            );
        }
    }

    /**
     * Validates active mentor status rule BR-MEN-03
     */
    public void validateMentorActive(Mentor mentor) {
        if (mentor.getStatus() != MentorStatus.ACTIVE) {
            throw new InvalidMentorAssignmentException(
                    String.format("Cannot assign mentor %s because status is %s", mentor.getFullName(), mentor.getStatus())
            );
        }
    }

    /**
     * Validates mentor capacity rule BR-MEN-04
     */
    public void validateMentorCapacity(Mentor mentor, Long tenantId) {
        long activeCount = mentorAssignmentRepository.countByTenantIdAndMentorIdAndStatus(
                tenantId, mentor.getId(), MentorAssignmentStatus.ACTIVE
        );
        if (activeCount >= mentor.getCapacity()) {
            throw new MentorCapacityExceededException(mentor.getId(), (int) activeCount, mentor.getCapacity());
        }
    }

    /**
     * Validates non-overlapping primary assignment rules BR-MEN-05 and BR-MEN-06
     */
    public void validatePrimaryAssignmentOverlap(Long tenantId, Long internId, LocalDate startDate, LocalDate endDate, Long currentAssignmentId) {
        List<MentorAssignment> activePrimaryAssignments = mentorAssignmentRepository.findActivePrimaryAssignmentsForIntern(
                tenantId, internId, MentorAssignmentStatus.ACTIVE, MentorAssignmentType.PRIMARY
        );

        for (MentorAssignment existing : activePrimaryAssignments) {
            if (currentAssignmentId != null && existing.getId().equals(currentAssignmentId)) {
                continue;
            }
            // Check for date range overlap
            LocalDate existingStart = existing.getStartDate();
            LocalDate existingEnd = existing.getEndDate();

            boolean overlaps = isDateRangeOverlapping(startDate, endDate, existingStart, existingEnd);
            if (overlaps) {
                throw new InvalidMentorAssignmentException(
                        String.format("Intern already has an active PRIMARY mentor assignment (%s - %s) with Mentor ID %d",
                                existingStart, existingEnd != null ? existingEnd : "ONGOING", existing.getMentor().getId())
                );
            }
        }
    }

    private boolean isDateRangeOverlapping(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        LocalDate actualEnd1 = (end1 != null) ? end1 : LocalDate.MAX;
        LocalDate actualEnd2 = (end2 != null) ? end2 : LocalDate.MAX;
        return !start1.isAfter(actualEnd2) && !actualEnd1.isBefore(start2);
    }
}
