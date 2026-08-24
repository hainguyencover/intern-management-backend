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
public class UniversityStatisticsReportGenerator implements ReportGenerator {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public String getReportCode() {
        return "UNIVERSITY_STATISTICS";
    }

    @Override
    public String getReportName() {
        return "Thống kê theo trường đại học";
    }

    @Override
    public String getDescription() {
        return "Thống kê số lượng thực tập sinh và tỷ lệ hoàn thành phân theo trường đại học";
    }

    @Override
    public String getCategory() {
        return "Academic Reports";
    }

    @Override
    public List<String> getAvailableFilters() {
        return List.of("programId", "universityId", "fromDate", "toDate");
    }

    @Override
    public ReportData generate(ReportFilterRequest filter, Long tenantId) {
        StringBuilder sql = new StringBuilder("""
            SELECT 
                COALESCE(u.name, 'Chưa xác định') AS universityName,
                COUNT(ip.id) AS totalInterns,
                SUM(CASE WHEN ip.status IN ('ACTIVE', 'IN_PROGRESS') THEN 1 ELSE 0 END) AS activeCount,
                SUM(CASE WHEN ip.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completedCount,
                SUM(CASE WHEN ip.status IN ('TERMINATED', 'DROPPED_OUT') THEN 1 ELSE 0 END) AS terminatedCount
            FROM intern_profiles ip
            LEFT JOIN universities u ON u.id = ip.university_id
            WHERE ip.tenant_id = :tenantId
        """);

        MapSqlParameterSource params = new MapSqlParameterSource("tenantId", tenantId);

        if (filter.getProgramId() != null) {
            sql.append(" AND ip.program_id = :programId");
            params.addValue("programId", filter.getProgramId());
        }
        if (filter.getUniversityId() != null) {
            sql.append(" AND ip.university_id = :universityId");
            params.addValue("universityId", filter.getUniversityId());
        }

        sql.append(" GROUP BY u.id, u.name ORDER BY totalInterns DESC");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params);

        int totalUnivs = rows.size();
        long totalInternsSum = 0;
        long totalCompletedSum = 0;

        List<String> columns = List.of(
            "Tên trường", "Tổng số TTS", "Đang thực tập", "Đã hoàn thành", "Đã nghỉ/bỏ học", "Tỷ lệ hoàn thành (%)"
        );

        List<Map<String, Object>> formattedRows = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            long total = ((Number) r.get("totalInterns")).longValue();
            long completed = ((Number) r.get("completedCount")).longValue();
            double rate = total > 0 ? (double) completed / total * 100 : 0.0;

            totalInternsSum += total;
            totalCompletedSum += completed;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Tên trường", r.get("universityName"));
            map.put("Tổng số TTS", total);
            map.put("Đang thực tập", r.get("activeCount"));
            map.put("Đã hoàn thành", completed);
            map.put("Đã nghỉ/bỏ học", r.get("terminatedCount"));
            map.put("Tỷ lệ hoàn thành (%)", String.format("%.1f%%", rate));
            formattedRows.add(map);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("Tổng số trường", totalUnivs);
        summary.put("Tổng số thực tập sinh", totalInternsSum);
        summary.put("Tổng số hoàn thành", totalCompletedSum);
        summary.put("Tỷ lệ hoàn thành chung", totalInternsSum > 0 ? String.format("%.1f%%", (double) totalCompletedSum / totalInternsSum * 100) : "0.0%");

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
