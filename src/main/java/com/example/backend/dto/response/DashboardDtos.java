package com.example.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

public class DashboardDtos {

    @Data
    @Builder
    public static class InternDashboardResponse {
        private String internName;
        private String position;
        private String mentorName;
        private int tasksCompleted;
        private int tasksTotal;
        private int daysInternship;
        private int totalDays;
        private String nextMeeting; // "Review Sprint 2 - 14:00 Today"
        private List<ActivityDto> recentActivities;
    }

    @Data
    @Builder
    public static class ActivityDto {
        private String content;
        private String timeAgo;
        private String color; // "blue", "green", "gray"
    }

    @Data
    @Builder
    public static class BirthdayDto {
        private String name;
        private String position; // "Inter - Major"
        private String date; // "15/03"
    }

    @Data
    @Builder
    public static class AlertDto {
        private String message;
        private String type; // "warning", "info", "success", "error"
    }
}
