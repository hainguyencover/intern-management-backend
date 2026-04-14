package com.holaho.intern.shared.dto.response;

import com.holaho.intern.shared.enums.LeaveType;


import com.holaho.intern.entity.LeaveRequest;
import com.holaho.intern.shared.enums.LeaveStatus;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
public class LeaveRequestResponse {
    private Long id;
    private Long internId;
    private String internName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private com.holaho.intern.shared.enums.LeaveType leaveType;
    private LeaveStatus status;
    private String approverName;
    private String rejectedReason;

    public static LeaveRequestResponse from(LeaveRequest request) {
        return LeaveRequestResponse.builder()
                .id(request.getId())
                .internId(request.getIntern().getId())
                .internName(request.getIntern().getUser().getFullName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .reason(request.getReason())
                .leaveType(request.getLeaveType())
                .status(request.getStatus())
                .approverName(request.getApprovedBy() != null ? request.getApprovedBy().getFullName() : null)
                .rejectedReason(request.getRejectedReason())
                .build();
    }
}

