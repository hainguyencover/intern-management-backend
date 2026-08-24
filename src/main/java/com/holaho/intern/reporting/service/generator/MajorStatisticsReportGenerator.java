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
public class MajorStatisticsReportGenerator implements ReportGenerator {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public String getReportCode() {
        return "MAJOR_STATISTICS";
    }

    @Override
    public String getReportName() {
        return "Thống kê theo chuyên ngành";
    }

    @Override
    public String getDescription() {
        return "Thống kê phân bổ thực tập sinh theo chuyên ngành đào tạo";
    }

    @Override
    public String getCategory() {
        return "Academic Reports";
    }

    @Override
    public List<String> getAvailableFilters() {
        return List.of("programId", "majorId", "fromDate", "toDate");
    }

    @Override
    public ReportData generate(ReportFilterRequest filter, Long tenantId) {
        StringBuilder sql = new StringBuilder("""
            SELECT 
                COALESCE(m.name, 'Chưa xác định') AS majorName,
                COUNT(ip.id) AS totalInterns,
                SUM(CASE WHEN ip.status IN ('ACTIVE', 'IN_PROGRESS') THEN 1 ELSE 0 END) AS activeCount,
                SUM(CASE WHEN ip.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completedCount,
                SUM(CASE WHEN ip.status IN ('TERMINATED', 'DROPPED_OUT') THEN 1 ELSE 0 END) AS terminatedCount
            FROM intern_profiles ip
            LEFT JOIN majors m ON m.id = ip.major_id
            WHERE ip.tenant_id = :tenantId
        """);

        MapSqlParameterSource params = new MapSqlParameterSource("tenantId", tenantId);

        if (filter.getProgramId() != null) {
            sql.append(" AND ip.program_id = :programId");
            params.addValue("programId", filter.getProgramId());
        }
        if (filter.getMajorId() != null) {
            sql.append(" AND ip.major_id = :majorId");
            params.addValue("majorId", filter.getMajorId());
        }

        sql.append(" GROUP BY m.id, m.name ORDER BY totalInterns DESC");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params);

        List<String> columns = List.of(
            "Chuyên ngành", "Tổng số TTS", "Đang thực tập", "Đã hoàn thành", "Đã nghỉ/bỏ học", "Tỷ lệ (%)"
        );

        long totalInternsSum = rows.stream().mapToLong(r -> ((Number) r.get("totalInterns")).longValue()).sum();

        List<Map<String, Object>> formattedRows = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            long total = ((Number) r.get("totalInterns")).longValue();
            double share = totalInternsSum > 0 ? (double) total / totalInternsSum * 100 : 0.0;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Chuyên ngành", r.get("majorName"));
            map.put("Tổng số TTS", total);
            map.put("Đang thực tập", r.get("activeCount"));
            map.put("Đã hoàn thành", r.get("completedCount"));
            map.put("Đã nghỉ/bỏ học", r.get("terminatedCount"));
            map.put("Tỷ lệ (%)", String.format("%.1f%%", share));
            formattedRows.add(map);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("Tổng số chuyên ngành", rows.size());
        summary.put("Tổng số thực tập sinh", totalInternsSum);

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
