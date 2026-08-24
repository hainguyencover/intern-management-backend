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
    name = "meeting_reminders",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_meeting_reminder", columnNames = {"meeting_id", "reminder_type"})
    }
)
public class MeetingReminder extends BaseEntity {

    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    @Column(name = "reminder_type", nullable = false, length = 30)
    private String reminderType;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";
}
