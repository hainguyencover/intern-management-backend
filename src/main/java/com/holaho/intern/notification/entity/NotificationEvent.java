package com.holaho.intern.notification.entity;

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
    name = "notification_events",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_event_id", columnNames = {"event_id"})
    }
)
public class NotificationEvent extends BaseEntity {

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;
}
