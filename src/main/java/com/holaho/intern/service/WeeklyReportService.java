package com.holaho.intern.service;

import com.holaho.intern.shared.dto.WeeklyReportDto;
import com.holaho.intern.shared.dto.request.WeeklyReportRequest;
import com.holaho.intern.shared.dto.request.ReviewWeeklyReportRequest;
import com.holaho.intern.shared.dto.response.FinalReportDto;
import com.holaho.intern.shared.dto.response.FinalReportSummaryDto;
import com.holaho.intern.shared.dto.InternCountStatDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WeeklyReportService {
    FinalReportDto getFinalReport(Long internId);
    List<WeeklyReportDto> getByIntern(Long internId);
    WeeklyReportDto internSubmit(Long internId, WeeklyReportRequest req);
    Page<WeeklyReportDto> internMyReports(Long internId, Pageable pageable);
    Page<WeeklyReportDto> mentorGroupReports(Long mentorUserId, Long groupId, Long internId, String status, Pageable pageable);
    WeeklyReportDto mentorReview(Long mentorUserId, Long reportId, ReviewWeeklyReportRequest req);
    WeeklyReportDto getReportDetail(Long id);
    WeeklyReportDto updateStatus(Long id, String status);
    List<FinalReportSummaryDto> getReportsSummary();
    List<InternCountStatDto> getStatsByAssessment();
}

