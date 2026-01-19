package com.example.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardOverviewResponse {
    private long totalInterns;
    private long totalMentors;
    private long totalPrograms;
    private long activeGroups;
    private long pendingApplications;
    private long documentsToReview;
    private Long activePrograms;
    private Double completionRate;
}
