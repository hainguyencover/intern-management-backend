package com.holaho.intern.notification.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.holaho.intern.notification.enums.NotificationStatus;
import com.holaho.intern.shared.entity.BaseEntity;
import com.holaho.intern.shared.enums.NotificationType;
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
    name = "notifications",
    indexes = {
        @Index(name = "idx_notification_recipient_status", columnList = "recipient_id,status"),
        @Index(name = "idx_notification_recipient_created", columnList = "recipient_id,created_at"),
        @Index(name = "idx_notification_reference", columnList = "reference_type,reference_id"),
        @Index(name = "idx_notification_university", columnList = "university_id,created_at")
    }
)
public class Notification extends BaseEntity {

    @Column(name = "university_id")
    private Long universityId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.UNREAD;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
