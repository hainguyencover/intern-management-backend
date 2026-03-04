package com.example.backend.dto.response;

import com.example.backend.dto.WeeklyReportDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MentorDashboardStats {
    private long totalInterns;
    private long activeTasks;
    private long reviewedReports;
    private List<TaskResponse> recentTasks;
    private List<WeeklyReportDto> recentReports;
}
