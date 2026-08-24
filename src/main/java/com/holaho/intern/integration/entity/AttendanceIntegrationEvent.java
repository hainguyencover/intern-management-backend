package com.holaho.intern.integration.entity;

import com.holaho.intern.integration.enums.EventProcessingStatus;
import com.holaho.intern.shared.entity.BaseEntity;
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
    name = "attendance_integration_events",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_attendance_external_event",
        columnNames = {"tenant_id", "connection_id", "external_event_id"}
    ),
    indexes = {
        @Index(name = "idx_attendance_event_employee_time", columnList = "external_employee_id, event_time"),
        @Index(name = "idx_attendance_event_status", columnList = "processing_status")
    }
)
public class AttendanceIntegrationEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "connection_id", nullable = false)
    private IntegrationConnection connection;

    @Column(name = "external_event_id", nullable = false, length = 255)
    private String externalEventId;

    @Column(name = "external_employee_id", nullable = false, length = 255)
    private String externalEmployeeId;

    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Column(name = "device_id", length = 255)
    private String deviceId;

    @Column(length = 30)
    private String method;

    @Column(name = "raw_payload", columnDefinition = "JSON")
    private String rawPayload;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 30)
    private EventProcessingStatus processingStatus;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
