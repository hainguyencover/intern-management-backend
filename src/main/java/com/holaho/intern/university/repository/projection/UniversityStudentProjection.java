package com.holaho.intern.university.repository.projection;

import java.math.BigDecimal;

public interface UniversityStudentProjection {
    Long getId();
    String getStudentCode();
    String getFullName();
    String getMajor();
    String getStatus();
    String getMentorName();
    String getProgramName();
    Long getTotalTasks();
    Long getCompletedTasks();
    Long getOverdueTasks();
    Long getWorkingDays();
    Long getPresentDays();
    Long getLeaveDays();
    BigDecimal getAttendanceRate();
    BigDecimal getOverallScore();
    String getEvaluationStatus();
}
