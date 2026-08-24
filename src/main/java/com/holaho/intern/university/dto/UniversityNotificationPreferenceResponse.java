package com.holaho.intern.university.dto;

import com.holaho.intern.notification.entity.UniversityNotificationPreference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniversityNotificationPreferenceResponse {
    private Long id;
    private Long universityId;
    private boolean internshipCompletedEnabled;
    private boolean internshipTerminatedEnabled;
    private boolean emailEnabled;
    private boolean inAppEnabled;

    public static UniversityNotificationPreferenceResponse fromEntity(UniversityNotificationPreference entity) {
        return UniversityNotificationPreferenceResponse.builder()
                .id(entity.getId())
                .universityId(entity.getUniversityId())
                .internshipCompletedEnabled(entity.isInternshipCompletedEnabled())
                .internshipTerminatedEnabled(entity.isInternshipTerminatedEnabled())
                .emailEnabled(entity.isEmailEnabled())
                .inAppEnabled(entity.isInAppEnabled())
                .build();
    }
}
