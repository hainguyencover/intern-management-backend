package com.holaho.intern.attendance.entity;

import com.holaho.intern.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "work_schedules",
    indexes = {
        @Index(name = "idx_work_schedules_tenant", columnList = "tenant_id, is_active")
    }
)
public class WorkSchedule extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "start_time", nullable = false)
    @Builder.Default
    private LocalTime startTime = LocalTime.of(8, 30);

    @Column(name = "end_time", nullable = false)
    @Builder.Default
    private LocalTime endTime = LocalTime.of(17, 30);

    @Column(name = "grace_period_minutes", nullable = false)
    @Builder.Default
    private Integer gracePeriodMinutes = 15;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
