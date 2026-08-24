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
public class ContractReportGenerator implements ReportGenerator {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public String getReportCode() {
        return "CONTRACT_REPORT";
    }

    @Override
    public String getReportName() {
        return "Báo cáo hợp đồng";
    }

    @Override
    public String getDescription() {
        return "Báo cáo trạng thái và vòng đời hợp đồng thực tập của toàn bộ sinh viên";
    }

    @Override
    public String getCategory() {
        return "Legal Reports";
    }

    @Override
    public List<String> getAvailableFilters() {
        return List.of("programId", "departmentId", "fromDate", "toDate");
    }

    @Override
    public ReportData generate(ReportFilterRequest filter, Long tenantId) {
        StringBuilder sql = new StringBuilder("""
            SELECT 
                c.contract_number AS contractNumber,
                ip.full_name AS internName,
                ip.student_code AS studentCode,
                c.contract_type AS contractType,
                c.start_date AS startDate,
                c.end_date AS endDate,
                c.status AS status,
                c.signed_at AS signedAt
            FROM contracts c
            JOIN intern_profiles ip ON ip.id = c.intern_id
            WHERE c.tenant_id = :tenantId
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
            sql.append(" AND c.start_date >= :fromDate");
            params.addValue("fromDate", filter.getFromDate());
        }
        if (filter.getToDate() != null) {
            sql.append(" AND c.end_date <= :toDate");
            params.addValue("toDate", filter.getToDate());
        }

        sql.append(" ORDER BY c.id DESC");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params);

        List<String> columns = List.of(
            "Số hợp đồng", "Họ và tên", "Mã sinh viên", "Loại hợp đồng", "Ngày hiệu lực", "Ngày hết hạn", "Trạng thái", "Ngày ký"
        );

        long activeContracts = rows.stream().filter(r -> "ACTIVE".equals(r.get("status")) || "SIGNED".equals(r.get("status"))).count();
        long expiredContracts = rows.stream().filter(r -> "EXPIRED".equals(r.get("status")) || "TERMINATED".equals(r.get("status"))).count();

        List<Map<String, Object>> formattedRows = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Số hợp đồng", r.get("contractNumber"));
            map.put("Họ và tên", r.get("internName"));
            map.put("Mã sinh viên", r.get("studentCode"));
            map.put("Loại hợp đồng", r.get("contractType") != null ? r.get("contractType") : "Hợp đồng thực tập");
            map.put("Ngày hiệu lực", r.get("startDate"));
            map.put("Ngày hết hạn", r.get("endDate"));
            map.put("Trạng thái", r.get("status"));
            map.put("Ngày ký", r.get("signedAt") != null ? r.get("signedAt") : "-");
            formattedRows.add(map);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("Tổng số hợp đồng", rows.size());
        summary.put("Hợp đồng có hiệu lực/Đã ký", activeContracts);
        summary.put("Hợp đồng hết hạn/Hủy", expiredContracts);

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
