package com.holaho.intern.entity;

import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.shared.entity.BaseEntity;
import com.holaho.intern.shared.enums.EnrollmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "internship_enrollments", indexes = {
        @Index(name = "idx_enrollment_intern", columnList = "intern_id"),
        @Index(name = "idx_enrollment_program", columnList = "program_id"),
        @Index(name = "idx_enrollment_status", columnList = "status")
})
public class InternshipEnrollment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private ProgramGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intern_id", nullable = false)
    private InternProfile intern;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    @Column(name = "joined_at", nullable = false)
    private LocalDate joinedAt;

    @Column(name = "ended_at")
    private LocalDate endedAt;
}
