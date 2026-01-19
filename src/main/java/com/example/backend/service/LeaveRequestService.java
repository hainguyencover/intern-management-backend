package com.example.backend.service;

import com.example.backend.entity.*;
import com.example.backend.enums.LeaveStatus;
import com.example.backend.enums.LeaveType;
import com.example.backend.exception.*;
import com.example.backend.repository.*;
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
                    com.example.backend.enums.NotificationType.TASK,
                    "Yêu cầu nghỉ phép mới",
                    "Thực tập sinh " + intern.getUser().getFullName() + " đã gửi yêu cầu nghỉ phép.");
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
                com.example.backend.enums.NotificationType.TASK, // Using closest enum
                "Đơn nghỉ phép được duyệt",
                "Đơn xin nghỉ từ " + request.getStartDate() + " đến " + request.getEndDate() + " đã được duyệt.");

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
