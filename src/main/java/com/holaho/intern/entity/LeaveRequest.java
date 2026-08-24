package com.holaho.intern.entity;

import com.holaho.intern.attendance.entity.LeaveType;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.user.entity.User;
import com.holaho.intern.shared.entity.BaseEntity;
import com.holaho.intern.shared.enums.LeaveStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "leave_requests")
public class LeaveRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intern_id", nullable = false)
    private InternProfile intern;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id")
    private LeaveType leaveTypeEntity;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "total_days", precision = 4, scale = 1)
    @Builder.Default
    private BigDecimal totalDays = new BigDecimal("1.0");

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "attachment_url", length = 1000)
    private String attachmentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", length = 20)
    private com.holaho.intern.shared.enums.LeaveType leaveType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    private String rejectedReason;
}


