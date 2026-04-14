package com.holaho.intern.shared.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class DashboardDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InternDashboardResponse {
        private String internName;
        private String position;
        private String mentorName;
        private int tasksCompleted;
        private int tasksTotal;
        private int daysInternship;
        private int totalDays;

        private List<ActivityDto> recentActivities;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityDto {
        private String content;
        private String timeAgo;
        private String color; // "blue", "green", "gray"
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BirthdayDto {
        private String name;
        private String position; // "Inter - Major"
        private String date; // "15/03"
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertDto {
        private String message;
        private String type; // "warning", "info", "success", "error"
    }
}

