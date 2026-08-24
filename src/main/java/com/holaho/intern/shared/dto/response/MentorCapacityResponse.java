package com.holaho.intern.shared.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentorCapacityResponse {
    private Long mentorId;
    private String mentorName;
    private String mentorEmail;
    private Long departmentId;
    private String departmentName;
    private long activeInternCount;
    @Builder.Default
    private int maxCapacity = 5;
    private boolean full;
    private double capacityPercentage;
}
