package com.holaho.intern.notification.entity;

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
    name = "university_notification_preferences",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_univ_notification_pref", columnNames = {"university_id"})
    }
)
public class UniversityNotificationPreference extends BaseEntity {

    @Column(name = "university_id", nullable = false)
    private Long universityId;

    @Column(name = "internship_completed_enabled", nullable = false)
    @Builder.Default
    private boolean internshipCompletedEnabled = true;

    @Column(name = "internship_terminated_enabled", nullable = false)
    @Builder.Default
    private boolean internshipTerminatedEnabled = true;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private boolean emailEnabled = true;

    @Column(name = "in_app_enabled", nullable = false)
    @Builder.Default
    private boolean inAppEnabled = true;
}
