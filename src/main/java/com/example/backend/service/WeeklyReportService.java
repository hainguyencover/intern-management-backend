package com.example.backend.service;

import com.example.backend.dto.WeeklyReportDto;
import com.example.backend.dto.request.WeeklyReportRequest;
import com.example.backend.dto.request.ReviewWeeklyReportRequest;
import com.example.backend.dto.response.FinalReportDto;
import com.example.backend.dto.response.FinalReportSummaryDto;
import com.example.backend.dto.InternCountStatDto;
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
