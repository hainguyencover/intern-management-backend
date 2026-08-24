package com.holaho.intern.notification.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.holaho.intern.notification.enums.DeliveryStatus;
import com.holaho.intern.notification.enums.NotificationChannel;
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
    name = "notification_deliveries",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_channel", columnNames = {"notification_id", "channel"})
    },
    indexes = {
        @Index(name = "idx_delivery_status", columnList = "status")
    }
)
public class NotificationDelivery extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
