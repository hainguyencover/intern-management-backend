package com.holaho.intern.university.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UniversityNotificationPreferenceUpdateRequest {
    private Boolean internshipCompletedEnabled;
    private Boolean internshipTerminatedEnabled;
    private Boolean emailEnabled;
    private Boolean inAppEnabled;
}
