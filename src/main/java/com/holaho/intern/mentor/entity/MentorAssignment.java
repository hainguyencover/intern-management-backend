package com.holaho.intern.mentor.entity;

import com.holaho.intern.entity.InternshipEnrollment;
import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.shared.entity.BaseEntity;
import com.holaho.intern.shared.enums.MentorAssignmentStatus;
import com.holaho.intern.shared.enums.MentorAssignmentType;
import com.holaho.intern.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "mentor_assignments", indexes = {
        @Index(name = "idx_assignment_tenant_mentor", columnList = "tenant_id, mentor_id"),
        @Index(name = "idx_assignment_tenant_intern", columnList = "tenant_id, intern_id"),
        @Index(name = "idx_assignment_tenant_status", columnList = "tenant_id, status"),
        @Index(name = "idx_assignment_tenant_dates", columnList = "tenant_id, start_date, end_date")
})
public class MentorAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id")
    private InternshipEnrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intern_id", nullable = false)
    private InternProfile intern;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mentor_id", nullable = false)
    private Mentor mentor;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false, length = 30)
    private MentorAssignmentType assignmentType = MentorAssignmentType.PRIMARY;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MentorAssignmentStatus status = MentorAssignmentStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String responsibility;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    @Column(name = "unassigned_at")
    private LocalDateTime unassignedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unassigned_by")
    private User unassignedBy;

    @Column(name = "unassign_reason", length = 500)
    private String unassignReason;

    @Column(length = 500)
    private String reason;

    @Version
    private Long version;
}
