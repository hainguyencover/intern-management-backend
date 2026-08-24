package com.holaho.intern.shared.workflow;

import com.holaho.intern.shared.enums.ApplicationStatus;
import com.holaho.intern.shared.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * State Machine Guard for Application Status transitions (BR-01).
 * Strictly prevents illegal jumps like DRAFT -> APPROVED.
 */
@Slf4j
@Component
public class ApplicationStateMachine {

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> LEGAL_TRANSITIONS = Map.of(
            ApplicationStatus.DRAFT, EnumSet.of(ApplicationStatus.SUBMITTED),
            ApplicationStatus.SUBMITTED, EnumSet.of(ApplicationStatus.REVIEWING, ApplicationStatus.SCREENING, ApplicationStatus.APPROVED, ApplicationStatus.REJECTED),
            ApplicationStatus.REVIEWING, EnumSet.of(ApplicationStatus.NEEDS_REVISION, ApplicationStatus.APPROVED, ApplicationStatus.REJECTED),
            ApplicationStatus.NEEDS_REVISION, EnumSet.of(ApplicationStatus.SUBMITTED),
            ApplicationStatus.SCREENING, EnumSet.of(ApplicationStatus.INTERVIEWING, ApplicationStatus.APPROVED, ApplicationStatus.REJECTED),
            ApplicationStatus.INTERVIEWING, EnumSet.of(ApplicationStatus.APPROVED, ApplicationStatus.REJECTED),
            ApplicationStatus.APPROVED, EnumSet.of(ApplicationStatus.CONTRACT_SENT, ApplicationStatus.INTERNING, ApplicationStatus.REJECTED),
            ApplicationStatus.CONTRACT_SENT, EnumSet.of(ApplicationStatus.CONTRACT_SIGNED, ApplicationStatus.REJECTED),
            ApplicationStatus.CONTRACT_SIGNED, EnumSet.of(ApplicationStatus.INTERNING),
            ApplicationStatus.REJECTED, EnumSet.noneOf(ApplicationStatus.class)
    );

    /**
     * Validates that transitioning from currentStatus to targetStatus complies with BR-01.
     * Throws BadRequestException if transition is illegal.
     */
    public void validateTransition(ApplicationStatus currentStatus, ApplicationStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return; // No-op transition
        }

        Set<ApplicationStatus> allowed = LEGAL_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(ApplicationStatus.class));
        if (!allowed.contains(targetStatus)) {
            log.error("Illegal state transition attempted: {} -> {}", currentStatus, targetStatus);
            throw new BadRequestException(String.format(
                    "Không thể chuyển trạng thái đơn từ '%s' sang '%s'. Quy trình không hợp lệ (BR-01).",
                    currentStatus, targetStatus));
        }

        log.info("Application state transition validated: {} -> {}", currentStatus, targetStatus);
    }
}
