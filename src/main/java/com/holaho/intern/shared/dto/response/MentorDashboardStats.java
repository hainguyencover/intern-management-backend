package com.holaho.intern.shared.dto.response;

import com.holaho.intern.shared.dto.WeeklyReportDto;
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

