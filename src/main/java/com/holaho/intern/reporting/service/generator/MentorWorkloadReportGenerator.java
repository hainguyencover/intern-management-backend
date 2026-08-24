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
public class MentorWorkloadReportGenerator implements ReportGenerator {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public String getReportCode() {
        return "MENTOR_WORKLOAD";
    }

    @Override
    public String getReportName() {
        return "Thống kê theo Mentor";
    }

    @Override
    public String getDescription() {
        return "Báo cáo tải công việc, định mức và số lượng thực tập sinh được hướng dẫn bởi từng Mentor";
    }

    @Override
    public String getCategory() {
        return "Mentor Reports";
    }

    @Override
    public List<String> getAvailableFilters() {
        return List.of("departmentId", "mentorId");
    }

    @Override
    public ReportData generate(ReportFilterRequest filter, Long tenantId) {
        StringBuilder sql = new StringBuilder("""
            SELECT 
                m.full_name AS mentorName,
                m.employee_code AS employeeCode,
                d.name AS departmentName,
                m.position AS position,
                m.capacity AS capacity,
                COUNT(DISTINCT ma.intern_id) AS currentInterns,
                SUM(CASE WHEN ip.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completedInterns
            FROM mentors m
            LEFT JOIN departments d ON d.id = m.department_id
            LEFT JOIN mentor_assignments ma ON ma.mentor_id = m.id AND ma.status = 'ACTIVE'
            LEFT JOIN intern_profiles ip ON ip.id = ma.intern_id
            WHERE m.tenant_id = :tenantId
        """);

        MapSqlParameterSource params = new MapSqlParameterSource("tenantId", tenantId);

        if (filter.getDepartmentId() != null) {
            sql.append(" AND m.department_id = :departmentId");
            params.addValue("departmentId", filter.getDepartmentId());
        }
        if (filter.getMentorId() != null) {
            sql.append(" AND m.id = :mentorId");
            params.addValue("mentorId", filter.getMentorId());
        }

        sql.append(" GROUP BY m.id, m.full_name, m.employee_code, d.name, m.position, m.capacity ORDER BY currentInterns DESC");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params);

        List<String> columns = List.of(
            "Mã Mentor", "Họ và tên", "Phòng ban", "Chức danh", "Định mức tối đa", "Đang hướng dẫn", "Hiệu suất tải (%)", "Đã hoàn thành"
        );

        long totalCapacitySum = 0;
        long totalAssignedSum = 0;
        int overloadedCount = 0;

        List<Map<String, Object>> formattedRows = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            int capacity = r.get("capacity") != null ? ((Number) r.get("capacity")).intValue() : 5;
            int assigned = ((Number) r.get("currentInterns")).intValue();
            double workload = capacity > 0 ? (double) assigned / capacity * 100 : 0.0;

            if (assigned > capacity) {
                overloadedCount++;
            }

            totalCapacitySum += capacity;
            totalAssignedSum += assigned;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Mã Mentor", r.get("employeeCode"));
            map.put("Họ và tên", r.get("mentorName"));
            map.put("Phòng ban", r.get("departmentName") != null ? r.get("departmentName") : "-");
            map.put("Chức danh", r.get("position") != null ? r.get("position") : "Mentor");
            map.put("Định mức tối đa", capacity);
            map.put("Đang hướng dẫn", assigned);
            map.put("Hiệu suất tải (%)", String.format("%.1f%%", workload));
            map.put("Đã hoàn thành", r.get("completedInterns"));
            formattedRows.add(map);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("Tổng số Mentor", rows.size());
        summary.put("Tổng định mức tải", totalCapacitySum);
        summary.put("Tổng TTS đang hướng dẫn", totalAssignedSum);
        summary.put("Số Mentor quá tải", overloadedCount);
        summary.put("Tỷ lệ lấp đầy trung bình", totalCapacitySum > 0 ? String.format("%.1f%%", (double) totalAssignedSum / totalCapacitySum * 100) : "0.0%");

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
