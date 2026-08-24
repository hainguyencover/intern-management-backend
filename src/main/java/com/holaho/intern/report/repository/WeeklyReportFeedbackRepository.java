package com.holaho.intern.report.repository;

import com.holaho.intern.report.entity.WeeklyReportFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeeklyReportFeedbackRepository extends JpaRepository<WeeklyReportFeedback, Long> {

    List<WeeklyReportFeedback> findByWeeklyReportIdOrderByCreatedAtAsc(Long weeklyReportId);
}
