package com.holaho.intern.reporting.service.generator;

import com.holaho.intern.reporting.dto.ReportData;
import com.holaho.intern.reporting.dto.ReportFilterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class AllowanceReportGenerator implements ReportGenerator {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public String getReportCode() {
        return "ALLOWANCE_REPORT";
    }

    @Override
    public String getReportName() {
        return "Báo cáo phụ cấp";
    }

    @Override
    public String getDescription() {
        return "Báo cáo thấu chi và thanh toán trợ cấp, phụ cấp cho thực tập sinh theo kỳ";
    }

    @Override
    public String getCategory() {
        return "Finance Reports";
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
                univ.name AS universityName,
                al.allowance_type AS allowanceType,
                al.base_amount AS baseAmount,
                al.working_days AS workingDays,
                al.actual_amount AS actualAmount,
                al.status AS status
            FROM allowances al
            JOIN intern_profiles ip ON ip.id = al.intern_id
            LEFT JOIN universities univ ON univ.id = ip.university_id
            WHERE al.tenant_id = :tenantId
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
            sql.append(" AND al.created_at >= :fromDate");
            params.addValue("fromDate", filter.getFromDate());
        }
        if (filter.getToDate() != null) {
            sql.append(" AND al.created_at <= :toDate");
            params.addValue("toDate", filter.getToDate());
        }

        sql.append(" ORDER BY al.id DESC");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params);

        List<String> columns = List.of(
            "Họ và tên", "Mã sinh viên", "Trường đại học", "Loại phụ cấp", "Mức phụ cấp chuẩn (VND)", "Số ngày tính công", "Thực nhận (VND)", "Trạng thái"
        );

        BigDecimal totalPayout = BigDecimal.ZERO;

        List<Map<String, Object>> formattedRows = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            BigDecimal actual = r.get("actualAmount") != null ? (BigDecimal) r.get("actualAmount") : BigDecimal.ZERO;
            BigDecimal base = r.get("baseAmount") != null ? (BigDecimal) r.get("baseAmount") : BigDecimal.ZERO;
            totalPayout = totalPayout.add(actual);

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Họ và tên", r.get("internName"));
            map.put("Mã sinh viên", r.get("studentCode"));
            map.put("Trường đại học", r.get("universityName") != null ? r.get("universityName") : "-");
            map.put("Loại phụ cấp", r.get("allowanceType") != null ? r.get("allowanceType") : "Phụ cấp hàng tháng");
            map.put("Mức phụ cấp chuẩn (VND)", String.format("%,.0f", base.doubleValue()));
            map.put("Số ngày tính công", r.get("workingDays") != null ? r.get("workingDays") : 0);
            map.put("Thực nhận (VND)", String.format("%,.0f", actual.doubleValue()));
            map.put("Trạng thái", r.get("status"));
            formattedRows.add(map);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("Tổng số bản ghi phụ cấp", rows.size());
        summary.put("Tổng chi trả phụ cấp (VND)", String.format("%,.0f", totalPayout.doubleValue()));
        summary.put("Mức chi trả trung bình (VND)", rows.size() > 0 ? String.format("%,.0f", totalPayout.doubleValue() / rows.size()) : "0");

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
