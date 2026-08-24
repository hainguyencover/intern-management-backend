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
public class ProgramSummaryReportGenerator implements ReportGenerator {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public String getReportCode() {
        return "PROGRAM_SUMMARY";
    }

    @Override
    public String getReportName() {
        return "Báo cáo tổng hợp chương trình thực tập";
    }

    @Override
    public String getDescription() {
        return "Báo cáo bức tranh toàn cảnh và tổng hợp về các chương trình thực tập trong hệ thống";
    }

    @Override
    public String getCategory() {
        return "Executive Reports";
    }

    @Override
    public List<String> getAvailableFilters() {
        return List.of("departmentId", "fromDate", "toDate");
    }

    @Override
    public ReportData generate(ReportFilterRequest filter, Long tenantId) {
        StringBuilder sql = new StringBuilder("""
            SELECT 
                p.code AS programCode,
                p.name AS programName,
                d.name AS departmentName,
                p.start_date AS startDate,
                p.end_date AS endDate,
                COUNT(DISTINCT ip.id) AS totalInterns,
                COUNT(DISTINCT ma.mentor_id) AS totalMentors,
                p.status AS status
            FROM programs p
            LEFT JOIN departments d ON d.id = p.department_id
            LEFT JOIN intern_profiles ip ON ip.program_id = p.id
            LEFT JOIN mentor_assignments ma ON ma.intern_id = ip.id AND ma.status = 'ACTIVE'
            WHERE p.tenant_id = :tenantId
        """);

        MapSqlParameterSource params = new MapSqlParameterSource("tenantId", tenantId);

        if (filter.getDepartmentId() != null) {
            sql.append(" AND p.department_id = :departmentId");
            params.addValue("departmentId", filter.getDepartmentId());
        }
        if (filter.getFromDate() != null) {
            sql.append(" AND p.start_date >= :fromDate");
            params.addValue("fromDate", filter.getFromDate());
        }
        if (filter.getToDate() != null) {
            sql.append(" AND p.end_date <= :toDate");
            params.addValue("toDate", filter.getToDate());
        }

        sql.append(" GROUP BY p.id, p.code, p.name, d.name, p.start_date, p.end_date, p.status ORDER BY p.id DESC");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params);

        List<String> columns = List.of(
            "Mã chương trình", "Tên chương trình", "Phòng ban phụ trách", "Ngày bắt đầu", "Ngày kết thúc", "Số lượng TTS", "Số lượng Mentor", "Trạng thái"
        );

        long grandTotalInterns = 0;
        long grandTotalMentors = 0;

        List<Map<String, Object>> formattedRows = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            long interns = ((Number) r.get("totalInterns")).longValue();
            long mentors = ((Number) r.get("totalMentors")).longValue();

            grandTotalInterns += interns;
            grandTotalMentors += mentors;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Mã chương trình", r.get("programCode"));
            map.put("Tên chương trình", r.get("programName"));
            map.put("Phòng ban phụ trách", r.get("departmentName") != null ? r.get("departmentName") : "-");
            map.put("Ngày bắt đầu", r.get("startDate"));
            map.put("Ngày kết thúc", r.get("endDate"));
            map.put("Số lượng TTS", interns);
            map.put("Số lượng Mentor", mentors);
            map.put("Trạng thái", r.get("status"));
            formattedRows.add(map);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("Tổng số chương trình", rows.size());
        summary.put("Tổng số thực tập sinh quản lý", grandTotalInterns);
        summary.put("Tổng số Mentor tham gia", grandTotalMentors);

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
