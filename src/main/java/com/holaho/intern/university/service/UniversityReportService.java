package com.holaho.intern.university.service;

import com.holaho.intern.shared.security.CustomUserDetails;
import com.holaho.intern.university.dto.UniversityStudentFilter;
import com.holaho.intern.university.dto.UniversityStudentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UniversityReportService {

    private final UniversityStudentService studentService;

    public Map<String, Object> getProgressReport(CustomUserDetails principal, String fromDate, String toDate) {
        Page<UniversityStudentResponse> page = studentService.findStudents(
                principal, new UniversityStudentFilter(), Pageable.unpaged()
        );
        List<UniversityStudentResponse> students = page.getContent();

        double avgProgress = students.stream()
                .mapToDouble(s -> s.getProgress().getCompletionRate().doubleValue())
                .average().orElse(0.0);

        double avgAttendance = students.stream()
                .mapToDouble(s -> s.getAttendance().getAttendanceRate().doubleValue())
                .average().orElse(100.0);

        long completedCount = students.stream()
                .filter(s -> "COMPLETED".equalsIgnoreCase(s.getStatus()))
                .count();

        double completionRate = students.isEmpty() ? 0.0 : (completedCount * 100.0) / students.size();

        Map<String, Object> report = new HashMap<>();
        Map<String, String> period = new HashMap<>();
        period.put("from", fromDate != null ? fromDate : "N/A");
        period.put("to", toDate != null ? toDate : "N/A");

        report.put("period", period);
        report.put("students", students.size());
        report.put("averageProgress", Math.round(avgProgress * 100.0) / 100.0);
        report.put("averageAttendance", Math.round(avgAttendance * 100.0) / 100.0);
        report.put("completionRate", Math.round(completionRate * 100.0) / 100.0);

        return report;
    }

    public byte[] exportProgressReportToExcel(CustomUserDetails principal) throws IOException {
        Page<UniversityStudentResponse> page = studentService.findStudents(
                principal, new UniversityStudentFilter(), Pageable.unpaged()
        );
        List<UniversityStudentResponse> students = page.getContent();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Student Progress Report");

            // Header Row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                    "MSSV", "Họ và tên", "Ngành học", "Trạng thái", "Chương trình", "Mentor",
                    "Tổng Task", "Task Hoàn thành", "Tỷ lệ Tiến độ (%)", "Chuyên cần (%)", "Điểm đánh giá"
            };

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (UniversityStudentResponse s : students) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(s.getStudentCode() != null ? s.getStudentCode() : "");
                row.createCell(1).setCellValue(s.getFullName() != null ? s.getFullName() : "");
                row.createCell(2).setCellValue(s.getMajor() != null ? s.getMajor() : "");
                row.createCell(3).setCellValue(s.getStatus() != null ? s.getStatus() : "");
                row.createCell(4).setCellValue(s.getProgramName() != null ? s.getProgramName() : "");
                row.createCell(5).setCellValue(s.getMentorName() != null ? s.getMentorName() : "");
                row.createCell(6).setCellValue(s.getProgress().getTotalTasks());
                row.createCell(7).setCellValue(s.getProgress().getCompletedTasks());
                row.createCell(8).setCellValue(s.getProgress().getCompletionRate().doubleValue());
                row.createCell(9).setCellValue(s.getAttendance().getAttendanceRate().doubleValue());
                row.createCell(10).setCellValue(
                        s.getEvaluation() != null && s.getEvaluation().getOverallScore() != null
                                ? s.getEvaluation().getOverallScore().doubleValue()
                                : 0.0
                );
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
