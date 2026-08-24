package com.holaho.intern.evaluation.export;

import com.holaho.intern.evaluation.dto.FinalReportDetailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * CSV / Excel Exporter for Final Evaluation Reports.
 */
@Component
@Slf4j
public class ExcelReportExporter implements ReportExporter {

    @Override
    public byte[] export(FinalReportDetailResponse report) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Report Number,Intern Name,Student Code,Email,Mentor,Evaluation Score (60%),Task Score (25%),Attendance Score (15%),Final Score,Classification,Status\n");
            sb.append(escape(report.getReportNumber())).append(",")
              .append(escape(report.getInternName())).append(",")
              .append(escape(report.getStudentCode())).append(",")
              .append(escape(report.getEmail())).append(",")
              .append(escape(report.getMentorName())).append(",")
              .append(report.getEvaluationScore()).append(",")
              .append(report.getTaskScore()).append(",")
              .append(report.getAttendanceScore()).append(",")
              .append(report.getFinalScore()).append(",")
              .append(escape(report.getClassification())).append(",")
              .append(report.getStatus()).append("\n");

            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to generate Excel report export: {}", e.getMessage(), e);
        }
        return out.toByteArray();
    }

    private String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    @Override
    public String getContentType() {
        return "text/csv";
    }

    @Override
    public String getFileExtension() {
        return ".csv";
    }
}
