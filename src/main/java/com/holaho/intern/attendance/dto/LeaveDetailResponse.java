package com.holaho.intern.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveDetailResponse {
    private Long id;
    private Long internId;
    private String internName;
    private String studentCode;

    private Long leaveTypeId;
    private String leaveTypeCode;
    private String leaveTypeName;

    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalDays;
    private String reason;
    private String attachmentUrl;

    private String status;
    private String approvedByName;
    private LocalDateTime reviewedAt;
    private String rejectedReason;
    private LocalDateTime createdAt;
}
