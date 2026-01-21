package com.example.backend.service;

import com.example.backend.dto.response.FinalEvaluationReportResponse;

public interface ReportService {
    FinalEvaluationReportResponse generateFinalEvaluationReport(String period);

    FinalEvaluationReportResponse generateFinalEvaluationReportByGroup(Long groupId, String period);

    byte[] exportFinalEvaluationReportExcel(FinalEvaluationReportResponse report);
}
