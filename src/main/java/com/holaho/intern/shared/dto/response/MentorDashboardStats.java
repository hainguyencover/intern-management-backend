package com.holaho.intern.shared.dto.response;

import com.holaho.intern.shared.dto.WeeklyReportDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentorDashboardStats {
    private long totalInterns;
    private long activeInterns;
    private long completedInterns;
    private long pendingReports;
    private long overdueReports;
    private long pendingTasks;
    private double averageInternProgress;
    private List<TaskResponse> recentTasks;
    private List<WeeklyReportDto> recentReports;
}
