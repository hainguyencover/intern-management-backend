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
public class CompletionRateReportGenerator implements ReportGenerator {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public String getReportCode() {
        return "COMPLETION_RATE";
    }

    @Override
    public String getReportName() {
        return "Tỷ lệ hoàn thành chương trình";
    }

    @Override
    public String getDescription() {
        return "Báo cáo thống kê tỷ lệ hoàn thành thực tập theo từng chương trình tuyển dụng";
    }

    @Override
    public String getCategory() {
        return "Performance Reports";
    }

    @Override
    public List<String> getAvailableFilters() {
        return List.of("programId", "departmentId", "fromDate", "toDate");
    }

    @Override
    public ReportData generate(ReportFilterRequest filter, Long tenantId) {
        StringBuilder sql = new StringBuilder("""
            SELECT 
                p.code AS programCode,
                p.name AS programName,
                d.name AS departmentName,
                COUNT(ip.id) AS totalEnrolled,
                SUM(CASE WHEN ip.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completedCount,
                SUM(CASE WHEN ip.status IN ('ACTIVE', 'IN_PROGRESS') THEN 1 ELSE 0 END) AS inProgressCount,
                SUM(CASE WHEN ip.status IN ('TERMINATED', 'DROPPED_OUT') THEN 1 ELSE 0 END) AS droppedOutCount
            FROM programs p
            LEFT JOIN departments d ON d.id = p.department_id
            LEFT JOIN intern_profiles ip ON ip.program_id = p.id
            WHERE p.tenant_id = :tenantId
        """);

        MapSqlParameterSource params = new MapSqlParameterSource("tenantId", tenantId);

        if (filter.getProgramId() != null) {
            sql.append(" AND p.id = :programId");
            params.addValue("programId", filter.getProgramId());
        }
        if (filter.getDepartmentId() != null) {
            sql.append(" AND p.department_id = :departmentId");
            params.addValue("departmentId", filter.getDepartmentId());
        }

        sql.append(" GROUP BY p.id, p.code, p.name, d.name ORDER BY totalEnrolled DESC");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params);

        List<String> columns = List.of(
            "Mã chương trình", "Tên chương trình", "Phòng ban", "Tổng tham gia", "Đã hoàn thành", "Đang thực hiện", "Bỏ học/Nghỉ", "Tỷ lệ hoàn thành (%)"
        );

        long grandTotalEnrolled = 0;
        long grandTotalCompleted = 0;

        List<Map<String, Object>> formattedRows = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            long total = ((Number) r.get("totalEnrolled")).longValue();
            long completed = ((Number) r.get("completedCount")).longValue();
            double rate = total > 0 ? (double) completed / total * 100 : 0.0;

            grandTotalEnrolled += total;
            grandTotalCompleted += completed;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Mã chương trình", r.get("programCode"));
            map.put("Tên chương trình", r.get("programName"));
            map.put("Phòng ban", r.get("departmentName") != null ? r.get("departmentName") : "-");
            map.put("Tổng tham gia", total);
            map.put("Đã hoàn thành", completed);
            map.put("Đang thực hiện", r.get("inProgressCount"));
            map.put("Bỏ học/Nghỉ", r.get("droppedOutCount"));
            map.put("Tỷ lệ hoàn thành (%)", String.format("%.1f%%", rate));
            formattedRows.add(map);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("Tổng số chương trình", rows.size());
        summary.put("Tổng số thực tập sinh", grandTotalEnrolled);
        summary.put("Tổng số đã hoàn thành", grandTotalCompleted);
        summary.put("Tỷ lệ hoàn thành trung bình", grandTotalEnrolled > 0 ? String.format("%.1f%%", (double) grandTotalCompleted / grandTotalEnrolled * 100) : "0.0%");

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
