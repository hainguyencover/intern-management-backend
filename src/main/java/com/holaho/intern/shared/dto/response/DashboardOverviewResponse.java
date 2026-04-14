package com.holaho.intern.shared.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewResponse {
    private long totalInterns;
    private long totalMentors;
    private long totalPrograms;
    private long activeGroups;
    private long pendingApplications;
    private long documentsToReview;
    private Long activePrograms;
    private Double completionRate;
    private long totalTasks;
    private long completedTasks;

    // Extra stats for enhanced dashboard
    private String systemHealth; // "GOOD", "WARNING", "CRITICAL"
    private String storageUsage; // "2.5 GB / 10 GB"
    private RecruitmentStats recruitmentStats;

    private List<DashboardDtos.ActivityDto> recentActivities;
    private List<DashboardDtos.AlertDto> systemAlerts;
    private List<DashboardDtos.BirthdayDto> upcomingBirthdays;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecruitmentStats {
        private long applied;
        private long interviewing;
        private long offerSent;
        private long onboarded;
    }
}

