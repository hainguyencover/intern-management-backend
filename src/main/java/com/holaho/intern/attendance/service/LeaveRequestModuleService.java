package com.holaho.intern.attendance.service;

import com.holaho.intern.attendance.dto.CreateLeaveRequestDto;
import com.holaho.intern.attendance.dto.LeaveDetailResponse;
import com.holaho.intern.attendance.dto.ReviewLeaveRequestDto;
import com.holaho.intern.attendance.entity.LeaveType;
import com.holaho.intern.attendance.repository.LeaveTypeRepository;
import com.holaho.intern.entity.LeaveRequest;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.intern.repository.InternProfileRepository;
import com.holaho.intern.repository.LeaveRequestRepository;
import com.holaho.intern.shared.config.TenantContext;
import com.holaho.intern.shared.enums.LeaveStatus;
import com.holaho.intern.shared.exception.BadRequestException;
import com.holaho.intern.shared.exception.NotFoundException;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service("leaveRequestModuleService")
@RequiredArgsConstructor
@Slf4j
public class LeaveRequestModuleService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final InternProfileRepository internProfileRepository;
    private final UserRepository userRepository;

    @Transactional
    public LeaveDetailResponse createLeaveRequest(Long internId, CreateLeaveRequestDto req) {
        Long tenantId = getCurrentTenantId();

        InternProfile intern = internProfileRepository.findById(internId)
                .orElseThrow(() -> new NotFoundException("Intern profile", internId));

        if (req.getStartDate().isAfter(req.getEndDate())) {
            throw new BadRequestException("Ngày bắt đầu không được lớn hơn ngày kết thúc.");
        }

        LeaveType leaveType = null;
        if (req.getLeaveTypeId() != null) {
            leaveType = leaveTypeRepository.findById(req.getLeaveTypeId())
                    .orElse(null);
        }

        LeaveRequest request = LeaveRequest.builder()
                .intern(intern)
                .leaveTypeEntity(leaveType)
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .totalDays(req.getTotalDays() != null ? req.getTotalDays() : new BigDecimal("1.0"))
                .reason(req.getReason())
                .attachmentUrl(req.getAttachmentUrl())
                .status(LeaveStatus.PENDING)
                .build();
        request.setTenantId(tenantId);

        request = leaveRequestRepository.save(request);
        log.info("Created leave request ID {} for intern {}", request.getId(), internId);

        return mapToDetailResponse(request);
    }

    @Transactional
    public LeaveDetailResponse approveLeaveRequest(Long requestId, Long hrUserId) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Leave request", requestId));

        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Đơn xin nghỉ phép không ở trạng thái chờ duyệt.");
        }

        User hrUser = userRepository.findById(hrUserId)
                .orElseThrow(() -> new NotFoundException("User", hrUserId));

        request.setStatus(LeaveStatus.APPROVED);
        request.setApprovedBy(hrUser);
        request.setReviewedAt(LocalDateTime.now());

        request = leaveRequestRepository.save(request);
        log.info("Approved leave request ID {} by HR {}", requestId, hrUserId);

        return mapToDetailResponse(request);
    }

    @Transactional
    public LeaveDetailResponse rejectLeaveRequest(Long requestId, Long hrUserId, ReviewLeaveRequestDto req) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Leave request", requestId));

        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("Đơn xin nghỉ phép không ở trạng thái chờ duyệt.");
        }

        User hrUser = userRepository.findById(hrUserId)
                .orElseThrow(() -> new NotFoundException("User", hrUserId));

        request.setStatus(LeaveStatus.REJECTED);
        request.setApprovedBy(hrUser);
        request.setReviewedAt(LocalDateTime.now());
        if (req != null && req.getRejectedReason() != null) {
            request.setRejectedReason(req.getRejectedReason());
        }

        request = leaveRequestRepository.save(request);
        log.info("Rejected leave request ID {} by HR {}", requestId, hrUserId);

        return mapToDetailResponse(request);
    }

    @Transactional(readOnly = true)
    public Page<LeaveDetailResponse> getLeaveRequests(LeaveStatus status, Pageable pageable) {
        Long tenantId = getCurrentTenantId();
        if (status != null) {
            return leaveRequestRepository.findByTenantIdAndStatus(tenantId, status, pageable)
                    .map(this::mapToDetailResponse);
        }
        return leaveRequestRepository.findByTenantId(tenantId, pageable)
                .map(this::mapToDetailResponse);
    }

    @Transactional(readOnly = true)
    public List<LeaveDetailResponse> getMyLeaveRequests(Long internId) {
        return leaveRequestRepository.findByInternId(internId).stream()
                .map(this::mapToDetailResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LeaveType> getLeaveTypes() {
        Long tenantId = getCurrentTenantId();
        return leaveTypeRepository.findByTenantIdAndIsActiveTrue(tenantId);
    }

    private LeaveDetailResponse mapToDetailResponse(LeaveRequest r) {
        return LeaveDetailResponse.builder()
                .id(r.getId())
                .internId(r.getIntern() != null ? r.getIntern().getId() : null)
                .internName(r.getIntern() != null && r.getIntern().getUser() != null ? r.getIntern().getUser().getFullName() : null)
                .studentCode(r.getIntern() != null ? r.getIntern().getStudentCode() : null)
                .leaveTypeId(r.getLeaveTypeEntity() != null ? r.getLeaveTypeEntity().getId() : null)
                .leaveTypeCode(r.getLeaveTypeEntity() != null ? r.getLeaveTypeEntity().getCode() : (r.getLeaveType() != null ? r.getLeaveType().name() : null))
                .leaveTypeName(r.getLeaveTypeEntity() != null ? r.getLeaveTypeEntity().getName() : null)
                .startDate(r.getStartDate())
                .endDate(r.getEndDate())
                .totalDays(r.getTotalDays())
                .reason(r.getReason())
                .attachmentUrl(r.getAttachmentUrl())
                .status(r.getStatus().name())
                .approvedByName(r.getApprovedBy() != null ? r.getApprovedBy().getFullName() : null)
                .reviewedAt(r.getReviewedAt())
                .rejectedReason(r.getRejectedReason())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private Long getCurrentTenantId() {
        Long tenantId = TenantContext.getCurrentTenantId();
        return tenantId != null ? tenantId : 1L;
    }
}
