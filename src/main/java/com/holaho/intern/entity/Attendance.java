package com.holaho.intern.entity;

import com.holaho.intern.intern.entity.InternProfile;
import com.holaho.intern.shared.entity.BaseEntity;

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
@Table(
    name = "attendances",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_attendance_intern_date",
        columnNames = {"tenant_id", "intern_id", "date"}
    ),
    indexes = {
        @Index(name = "idx_attendances_date", columnList = "date"),
        @Index(name = "idx_attendances_tenant_date", columnList = "tenant_id, date")
    }
)
public class Attendance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "intern_id", nullable = false)
    private InternProfile intern;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "check_in")
    private LocalDateTime checkIn;

    @Column(name = "check_out")
    private LocalDateTime checkOut;

    @Column(name = "scheduled_start_at")
    private LocalDateTime scheduledStartAt;

    @Column(name = "scheduled_end_at")
    private LocalDateTime scheduledEndAt;

    @Column(name = "total_minutes")
    private Integer totalMinutes;

    @Column(name = "worked_minutes")
    @Builder.Default
    private Integer workedMinutes = 0;

    @Column(name = "late_minutes")
    @Builder.Default
    private Integer lateMinutes = 0;

    @Column(name = "early_leave_minutes")
    @Builder.Default
    private Integer earlyLeaveMinutes = 0;

    @Column(name = "check_in_method", length = 30)
    @Builder.Default
    private String checkInMethod = "WEB";

    @Column(name = "check_out_method", length = 30)
    @Builder.Default
    private String checkOutMethod = "WEB";

    @Column(length = 1000)
    private String note;

    @Column(length = 30)
    private String status; // PRESENT, LATE, EARLY_LEAVE, LATE_AND_EARLY_LEAVE, ABSENT, ON_LEAVE, HOLIDAY, INCOMPLETE, PENDING_CORRECTION

    @Column(name = "source_type", length = 30)
    @Builder.Default
    private String sourceType = "MANUAL"; // MANUAL, QR, CARD, BIOMETRIC

    @Column(name = "external_event_id", length = 255)
    private String externalEventId;
}


