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
public class EvaluationReportGenerator implements ReportGenerator {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public String getReportCode() {
        return "EVALUATION_REPORT";
    }

    @Override
    public String getReportName() {
        return "Báo cáo đánh giá";
    }

    @Override
    public String getDescription() {
        return "Báo cáo điểm đánh giá chuyên môn, kỹ năng mềm và thái độ của thực tập sinh";
    }

    @Override
    public String getCategory() {
        return "Performance Reports";
    }

    @Override
    public List<String> getAvailableFilters() {
        return List.of("programId", "departmentId", "mentorId", "fromDate", "toDate");
    }

    @Override
    public ReportData generate(ReportFilterRequest filter, Long tenantId) {
        StringBuilder sql = new StringBuilder("""
            SELECT 
                ip.full_name AS internName,
                ip.student_code AS studentCode,
                mtr.full_name AS mentorName,
                e.technical_score AS techScore,
                e.soft_skills_score AS softScore,
                e.attitude_score AS attitudeScore,
                e.final_score AS finalScore,
                e.grade AS grade,
                e.result AS result
            FROM evaluations e
            JOIN intern_profiles ip ON ip.id = e.intern_id
            LEFT JOIN mentors mtr ON mtr.id = e.evaluator_mentor_id
            WHERE e.tenant_id = :tenantId
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
        if (filter.getMentorId() != null) {
            sql.append(" AND e.evaluator_mentor_id = :mentorId");
            params.addValue("mentorId", filter.getMentorId());
        }

        sql.append(" ORDER BY e.id DESC");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params);

        List<String> columns = List.of(
            "Họ và tên", "Mã sinh viên", "Mentor đánh giá", "Điểm kỹ thuật", "Kỹ năng mềm", "Thái độ", "Điểm tổng kết", "Xếp loại", "Kết quả"
        );

        long passCount = rows.stream().filter(r -> "PASS".equals(r.get("result")) || "PASSED".equals(r.get("result"))).count();
        double sumFinalScore = rows.stream().mapToDouble(r -> r.get("finalScore") != null ? ((Number) r.get("finalScore")).doubleValue() : 0.0).sum();

        List<Map<String, Object>> formattedRows = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            double finalScore = r.get("finalScore") != null ? ((Number) r.get("finalScore")).doubleValue() : 0.0;

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Họ và tên", r.get("internName"));
            map.put("Mã sinh viên", r.get("studentCode"));
            map.put("Mentor đánh giá", r.get("mentorName") != null ? r.get("mentorName") : "-");
            map.put("Điểm kỹ thuật", r.get("techScore") != null ? r.get("techScore") : 0.0);
            map.put("Kỹ năng mềm", r.get("softScore") != null ? r.get("softScore") : 0.0);
            map.put("Thái độ", r.get("attitudeScore") != null ? r.get("attitudeScore") : 0.0);
            map.put("Điểm tổng kết", String.format("%.2f", finalScore));
            map.put("Xếp loại", r.get("grade") != null ? r.get("grade") : "-");
            map.put("Kết quả", r.get("result") != null ? r.get("result") : "CHƯA XÁC ĐỊNH");
            formattedRows.add(map);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("Tổng số bản ghi đánh giá", rows.size());
        summary.put("Số lượng ĐẠT (PASS)", passCount);
        summary.put("Tỷ lệ ĐẠT (%)", rows.size() > 0 ? String.format("%.1f%%", (double) passCount / rows.size() * 100) : "0.0%");
        summary.put("Điểm trung bình toàn khóa", rows.size() > 0 ? String.format("%.2f", sumFinalScore / rows.size()) : "0.00");

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
