package com.holaho.intern.attendance.entity;

import com.holaho.intern.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "attendance_policies",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_attendance_policy_tenant",
        columnNames = {"tenant_id"}
    )
)
public class AttendancePolicy extends BaseEntity {

    @Column(name = "required_daily_minutes", nullable = false)
    @Builder.Default
    private Integer requiredDailyMinutes = 480;

    @Column(name = "grace_period_minutes", nullable = false)
    @Builder.Default
    private Integer gracePeriodMinutes = 15;

    @Column(name = "allow_late", nullable = false)
    @Builder.Default
    private Boolean allowLate = true;

    @Column(name = "allow_early_leave", nullable = false)
    @Builder.Default
    private Boolean allowEarlyLeave = true;

    @Column(name = "require_checkout", nullable = false)
    @Builder.Default
    private Boolean requireCheckout = true;

    @Column(name = "max_correction_days", nullable = false)
    @Builder.Default
    private Integer maxCorrectionDays = 3;

    @Column(name = "auto_mark_absent", nullable = false)
    @Builder.Default
    private Boolean autoMarkAbsent = true;
}
