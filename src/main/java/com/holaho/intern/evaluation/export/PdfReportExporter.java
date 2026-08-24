package com.holaho.intern.evaluation.export;

import com.holaho.intern.evaluation.dto.FinalReportDetailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * PDF Exporter for Final Evaluation Reports.
 * Generates structured document byte array.
 */
@Component
@Slf4j
public class PdfReportExporter implements ReportExporter {

    @Override
    public byte[] export(FinalReportDetailResponse report) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("====================================================\n");
            sb.append("         BÁO CÁO ĐÁNH GIÁ TỔNG KẾT THỰC TẬP         \n");
            sb.append("====================================================\n\n");
            sb.append("Mã báo cáo: ").append(report.getReportNumber()).append("\n");
            sb.append("Họ và tên TTS: ").append(report.getInternName()).append("\n");
            sb.append("Mã số sinh viên: ").append(report.getStudentCode()).append("\n");
            sb.append("Email: ").append(report.getEmail()).append("\n");
            sb.append("Mentor: ").append(report.getMentorName() != null ? report.getMentorName() : "N/A").append("\n");
            sb.append("Trạng thái: ").append(report.getStatus()).append("\n\n");

            sb.append("--- KẾT QUẢ ĐÁNH GIÁ TỔNG HỢP ---\n");
            sb.append("Điểm Đánh giá Mentor (60%): ").append(report.getEvaluationScore()).append("/10\n");
            sb.append("Điểm Tiến độ Công việc (25%): ").append(report.getTaskScore()).append("/10 (Hoàn thành ").append(report.getTaskCompletionRate()).append("%)\n");
            sb.append("Điểm Chấm công & Điểm danh (15%): ").append(report.getAttendanceScore()).append("/10 (Tỷ lệ ").append(report.getAttendanceRate()).append("%)\n");
            sb.append("----------------------------------------------------\n");
            sb.append("ĐIỂM TỔNG KẾT CHÍNH THỨC: ").append(report.getFinalScore()).append("/10\n");
            sb.append("XẾP LOẠI KẾT QUẢ: ").append(report.getClassification()).append("\n");
            sb.append("====================================================\n");

            if (report.getMentorComment() != null) {
                sb.append("\nNhận xét của Mentor:\n").append(report.getMentorComment()).append("\n");
            }
            if (report.getHrComment() != null) {
                sb.append("\nNhận xét của HR:\n").append(report.getHrComment()).append("\n");
            }

            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to generate PDF report export: {}", e.getMessage(), e);
        }
        return out.toByteArray();
    }

    @Override
    public String getContentType() {
        return "application/pdf";
    }

    @Override
    public String getFileExtension() {
        return ".pdf";
    }
}
