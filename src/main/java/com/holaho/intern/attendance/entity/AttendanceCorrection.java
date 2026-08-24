package com.holaho.intern.attendance.entity;

import com.holaho.intern.attendance.enums.CorrectionStatus;
import com.holaho.intern.entity.Attendance;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.shared.entity.BaseEntity;
import com.holaho.intern.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "attendance_corrections",
    indexes = {
        @Index(name = "idx_correction_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_correction_intern", columnList = "intern_id")
    }
)
public class AttendanceCorrection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attendance_id", nullable = false)
    private Attendance attendance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intern_id", nullable = false)
    private InternProfile intern;

    @Column(name = "requested_check_in")
    private LocalDateTime requestedCheckIn;

    @Column(name = "requested_check_out")
    private LocalDateTime requestedCheckOut;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private CorrectionStatus status = CorrectionStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_comment", length = 2000)
    private String reviewComment;
}
