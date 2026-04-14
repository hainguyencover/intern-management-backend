package com.holaho.intern.service;

import com.holaho.intern.entity.InternProfile;
import com.holaho.intern.repository.InternProfileRepository;
import com.holaho.intern.entity.LeaveRequest;
import com.holaho.intern.repository.LeaveRequestRepository;
import com.holaho.intern.entity.Mentor;
import com.holaho.intern.service.NotificationService;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import com.holaho.intern.shared.enums.NotificationType;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.NotFoundException;


import com.holaho.intern.shared.enums.LeaveStatus;
import com.holaho.intern.shared.enums.LeaveType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final InternProfileRepository internProfileRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public LeaveRequest createLeaveRequest(Long internId, LocalDate startDate,
            LocalDate endDate, String reason, LeaveType leaveType) {
        InternProfile intern = internProfileRepository.findById(internId)
                .orElseThrow(() -> new NotFoundException("Intern not found"));

        if (startDate.isAfter(endDate)) {
            throw new BadRequestException("Start date must be before end date");
        }

        if (startDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot request leave for past dates");
        }

        if (leaveRequestRepository.existsByInternAndDateOverlap(internId, startDate, endDate)) {
            throw new BadRequestException("You already have a pending or approved leave request in this period");
        }

        LeaveRequest request = new LeaveRequest();
        request.setIntern(intern);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setReason(reason);
        request.setLeaveType(leaveType);
        request.setStatus(LeaveStatus.PENDING);

        LeaveRequest savedRequest = leaveRequestRepository.save(request);

        // Notify Mentor (if any) or just log for HR (HR usually checks dashboard)
        if (intern.getMentor() != null) {
            notificationService.createNotification(
                    intern.getMentor().getUser().getId(),
                    com.holaho.intern.shared.enums.NotificationType.TASK,
                    "YÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Âªu cÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚ÂºÃƒâ€šÃ‚Â§u nghÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â° phÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©p mÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»ÃƒÂ¢Ã¢â€šÂ¬Ã‚Âºi",
                    "ThÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»Ãƒâ€šÃ‚Â±c tÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚ÂºÃƒâ€šÃ‚Â­p sinh " + intern.getUser().getFullName() + " ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â€šÂ¬Ã‹Å“ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â£ gÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»Ãƒâ€šÃ‚Â­i yÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Âªu cÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚ÂºÃƒâ€šÃ‚Â§u nghÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â° phÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©p.");
        }

        return savedRequest;
    }

    @Transactional
    public LeaveRequest approveLeaveRequest(Long requestId, Long approverId) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Leave request not found"));

        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Leave request is not pending");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new NotFoundException("Approver not found"));

        request.setStatus(LeaveStatus.APPROVED);
        request.setApprovedBy(approver);

        LeaveRequest saved = leaveRequestRepository.save(request);

        // Notify Intern
        notificationService.createNotification(
                request.getIntern().getUser().getId(),
                com.holaho.intern.shared.enums.NotificationType.TASK, // Using closest enum
                "ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬Â Ãƒâ€šÃ‚Â¡n nghÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â° phÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â©p ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â€šÂ¬Ã‹Å“ÃƒÆ’Ã¢â‚¬Â Ãƒâ€šÃ‚Â°ÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»Ãƒâ€šÃ‚Â£c duyÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡t",
                "ÃƒÆ’Ã¢â‚¬Å¾Ãƒâ€šÃ‚ÂÃƒÆ’Ã¢â‚¬Â Ãƒâ€šÃ‚Â¡n xin nghÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â° tÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»Ãƒâ€šÃ‚Â« " + request.getStartDate() + " ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â€šÂ¬Ã‹Å“ÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚ÂºÃƒâ€šÃ‚Â¿n " + request.getEndDate() + " ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â€šÂ¬Ã‹Å“ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â£ ÃƒÆ’Ã¢â‚¬Å¾ÃƒÂ¢Ã¢â€šÂ¬Ã‹Å“ÃƒÆ’Ã¢â‚¬Â Ãƒâ€šÃ‚Â°ÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»Ãƒâ€šÃ‚Â£c duyÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»ÃƒÂ¢Ã¢â€šÂ¬Ã‚Â¡t.");

        return saved;
    }

    @Transactional
    public LeaveRequest rejectLeaveRequest(Long requestId, Long approverId, String reason) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Leave request not found"));

        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Leave request is not pending");
        }

        User approver = userRepository.findById(approverId)
                .orElseThrow(() -> new NotFoundException("Approver not found"));

        request.setStatus(LeaveStatus.REJECTED);
        request.setApprovedBy(approver);
        request.setRejectedReason(reason);

        return leaveRequestRepository.save(request);
    }

    @Transactional(readOnly = true)
    public Page<LeaveRequest> getMyLeaveRequests(Long internId, Pageable pageable) {
        return leaveRequestRepository.findByIntern_Id(internId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<LeaveRequest> getPendingLeaveRequests(Pageable pageable) {
        return leaveRequestRepository.findByStatus(LeaveStatus.PENDING, pageable);
    }

    @Transactional(readOnly = true)
    public Page<LeaveRequest> searchLeaveRequests(Long internId, LeaveStatus status,
            Pageable pageable) {
        return leaveRequestRepository.search(internId, status, pageable);
    }
}

