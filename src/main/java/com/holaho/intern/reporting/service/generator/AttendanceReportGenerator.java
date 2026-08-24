package com.holaho.intern.reporting.service.generator;

import com.holaho.intern.reporting.dto.ReportData;
import com.holaho.intern.reporting.dto.ReportFilterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class AttendanceReportGenerator implements ReportGenerator {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public String getReportCode() {
        return "ATTENDANCE_REPORT";
    }

    @Override
    public String getReportName() {
        return "Báo cáo chuyên cần";
    }

    @Override
    public String getDescription() {
        return "Thống kê tình hình điểm danh, đi muộn, về sớm và nghỉ phép của thực tập sinh";
    }

    @Override
    public String getCategory() {
        return "Operations Reports";
    }

    @Override
    public List<String> getAvailableFilters() {
        return List.of("programId", "departmentId", "fromDate", "toDate");
    }

    @Override
    public ReportData generate(ReportFilterRequest filter, Long tenantId) {
        StringBuilder sql = new StringBuilder("""
            SELECT 
                ip.full_name AS internName,
                ip.student_code AS studentCode,
                d.name AS departmentName,
                COUNT(a.id) AS totalLogs,
                SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) AS presentDays,
                SUM(CASE WHEN a.status = 'LATE' THEN 1 ELSE 0 END) AS lateDays,
                SUM(CASE WHEN a.status = 'EARLY_LEAVE' THEN 1 ELSE 0 END) AS earlyLeaveDays,
                SUM(CASE WHEN a.status = 'ABSENT' THEN 1 ELSE 0 END) AS absentDays
            FROM intern_profiles ip
            LEFT JOIN departments d ON d.id = ip.department_id
            LEFT JOIN attendances a ON a.intern_id = ip.id
            WHERE ip.tenant_id = :tenantId
        """);

        MapSqlParameterSource params = new MapSqlParameterSource("tenantId", tenantId);

        if (filter.getProgramId() != null) {
            sql.append(" AND ip.program_id = :programId");
            params.addValue("programId", filter.getProgramId());
        }
        if (filter.getDepartmentId() != null) {
            sql.append(" AND ip.department_id = :departmentId");
            params.addValue("departmentId", filter.getDepartmentId());
        }
        if (filter.getFromDate() != null) {
            sql.append(" AND a.work_date >= :fromDate");
            params.addValue("fromDate", filter.getFromDate());
        }
        if (filter.getToDate() != null) {
            sql.append(" AND a.work_date <= :toDate");
            params.addValue("toDate", filter.getToDate());
        }

        sql.append(" GROUP BY ip.id, ip.full_name, ip.student_code, d.name ORDER BY totalLogs DESC");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params);

        List<String> columns = List.of(
            "Họ và tên", "Mã sinh viên", "Phòng ban", "Tổng số ngày ghi nhận", "Đúng giờ", "Đi muộn", "Về sớm", "Vắng mặt", "Tỷ lệ chuyên cần (%)"
        );

        long grandTotalLogs = 0;
        long grandTotalPresent = 0;

        List<Map<String, Object>> formattedRows = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            long total = ((Number) r.get("totalLogs")).longValue();
            long present = ((Number) r.get("presentDays")).longValue();
            double rate = total > 0 ? (double) present / total * 100 : 0.0;

            grandTotalLogs += total;
            grandTotalPresent += present;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Họ và tên", r.get("internName"));
            map.put("Mã sinh viên", r.get("studentCode"));
            map.put("Phòng ban", r.get("departmentName") != null ? r.get("departmentName") : "-");
            map.put("Tổng số ngày ghi nhận", total);
            map.put("Đúng giờ", present);
            map.put("Đi muộn", r.get("lateDays"));
            map.put("Về sớm", r.get("earlyLeaveDays"));
            map.put("Vắng mặt", r.get("absentDays"));
            map.put("Tỷ lệ chuyên cần (%)", String.format("%.1f%%", rate));
            formattedRows.add(map);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("Tổng số thực tập sinh", rows.size());
        summary.put("Tổng số ca/ngày làm việc", grandTotalLogs);
        summary.put("Tỷ lệ chuyên cần trung bình", grandTotalLogs > 0 ? String.format("%.1f%%", (double) grandTotalPresent / grandTotalLogs * 100) : "0.0%");

        return ReportData.builder()
            .reportCode(getReportCode())
            .reportName(getReportName())
            .category(getCategory())
            .columns(columns)
            .rows(formattedRows)
            .summary(summary)
            .generatedAt(LocalDateTime.now())
            .build();
    }
}
