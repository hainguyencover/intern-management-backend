package com.holaho.intern.university.dto;

import com.holaho.intern.notification.entity.Notification;
import com.holaho.intern.notification.enums.NotificationStatus;
import com.holaho.intern.shared.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniversityNotificationResponse {
    private Long id;
    private Long universityId;
    private NotificationType type;
    private String title;
    private String message;
    private String referenceType;
    private Long referenceId;
    private NotificationStatus status;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public static UniversityNotificationResponse fromEntity(Notification entity) {
        return UniversityNotificationResponse.builder()
                .id(entity.getId())
                .universityId(entity.getUniversityId())
                .type(entity.getType())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .referenceType(entity.getReferenceType())
                .referenceId(entity.getReferenceId())
                .status(entity.getStatus())
                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
