package com.holaho.intern.report.service;

import com.holaho.intern.report.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WeeklyReportService {

    WeeklyReportResponse createDraft(Long internUserId, WeeklyReportCreateRequest request);

    WeeklyReportResponse updateDraft(Long reportId, Long internUserId, WeeklyReportUpdateRequest request);

    WeeklyReportResponse submitReport(Long reportId, Long internUserId);

    WeeklyReportResponse getMyReportById(Long reportId, Long internUserId);

    Page<WeeklyReportResponse> getMyReports(Long internUserId, Pageable pageable);

    WeeklyReportResponse getMentorReportById(Long reportId, Long mentorUserId);

    Page<WeeklyReportResponse> getMentorReports(Long mentorUserId, WeeklyReportFilterRequest filter, Pageable pageable);

    WeeklyReportFeedbackResponse addFeedback(Long reportId, Long mentorUserId, WeeklyReportFeedbackRequest request);
}
