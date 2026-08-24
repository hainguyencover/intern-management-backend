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
public class InternListReportGenerator implements ReportGenerator {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public String getReportCode() {
        return "INTERN_LIST";
    }

    @Override
    public String getReportName() {
        return "Danh sách thực tập sinh";
    }

    @Override
    public String getDescription() {
        return "Báo cáo chi tiết danh sách thực tập sinh theo các tiêu chí tìm kiếm";
    }

    @Override
    public String getCategory() {
        return "HR Reports";
    }

    @Override
    public List<String> getAvailableFilters() {
        return List.of("programId", "departmentId", "universityId", "majorId", "mentorId", "internStatus", "fromDate", "toDate");
    }

    @Override
    public ReportData generate(ReportFilterRequest filter, Long tenantId) {
        StringBuilder sql = new StringBuilder("""
            SELECT 
                ip.full_name AS fullName,
                ip.student_code AS studentCode,
                u.email AS email,
                u.phone AS phone,
                univ.name AS university,
                m.name AS major,
                d.name AS department,
                mtr.full_name AS mentorName,
                ip.status AS status,
                p.name AS programName,
                ip.start_date AS startDate,
                ip.end_date AS endDate
            FROM intern_profiles ip
            JOIN users u ON u.id = ip.user_id
            LEFT JOIN universities univ ON univ.id = ip.university_id
            LEFT JOIN majors m ON m.id = ip.major_id
            LEFT JOIN departments d ON d.id = ip.department_id
            LEFT JOIN programs p ON p.id = ip.program_id
            LEFT JOIN mentor_assignments ma ON ma.intern_id = ip.id AND ma.status = 'ACTIVE'
            LEFT JOIN mentors mtr ON mtr.id = ma.mentor_id
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
        if (filter.getUniversityId() != null) {
            sql.append(" AND ip.university_id = :universityId");
            params.addValue("universityId", filter.getUniversityId());
        }
        if (filter.getMajorId() != null) {
            sql.append(" AND ip.major_id = :majorId");
            params.addValue("majorId", filter.getMajorId());
        }
        if (filter.getMentorId() != null) {
            sql.append(" AND ma.mentor_id = :mentorId");
            params.addValue("mentorId", filter.getMentorId());
        }
        if (filter.getInternStatus() != null && !filter.getInternStatus().isBlank()) {
            sql.append(" AND ip.status = :status");
            params.addValue("status", filter.getInternStatus());
        }
        if (filter.getFromDate() != null) {
            sql.append(" AND ip.start_date >= :fromDate");
            params.addValue("fromDate", filter.getFromDate());
        }
        if (filter.getToDate() != null) {
            sql.append(" AND ip.end_date <= :toDate");
            params.addValue("toDate", filter.getToDate());
        }

        sql.append(" ORDER BY ip.id DESC");

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), params);

        int total = rows.size();
        long activeCount = rows.stream().filter(r -> "ACTIVE".equals(r.get("status")) || "IN_PROGRESS".equals(r.get("status"))).count();
        long completedCount = rows.stream().filter(r -> "COMPLETED".equals(r.get("status"))).count();
        long terminatedCount = rows.stream().filter(r -> "TERMINATED".equals(r.get("status")) || "DROPPED_OUT".equals(r.get("status"))).count();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("Tổng thực tập sinh", total);
        summary.put("Đang thực tập", activeCount);
        summary.put("Đã hoàn thành", completedCount);
        summary.put("Đã nghỉ/bỏ học", terminatedCount);

        List<String> columns = List.of(
            "Họ và tên", "Mã sinh viên", "Email", "Số điện thoại",
            "Trường đại học", "Chuyên ngành", "Phòng ban", "Mentor",
            "Trạng thái", "Chương trình", "Ngày bắt đầu", "Ngày kết thúc"
        );

        List<Map<String, Object>> formattedRows = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("Họ và tên", r.get("fullName"));
            map.put("Mã sinh viên", r.get("studentCode"));
            map.put("Email", r.get("email"));
            map.put("Số điện thoại", r.get("phone"));
            map.put("Trường đại học", r.get("university") != null ? r.get("university") : "-");
            map.put("Chuyên ngành", r.get("major") != null ? r.get("major") : "-");
            map.put("Phòng ban", r.get("department") != null ? r.get("department") : "-");
            map.put("Mentor", r.get("mentorName") != null ? r.get("mentorName") : "Chưa phân công");
            map.put("Trạng thái", r.get("status"));
            map.put("Chương trình", r.get("programName") != null ? r.get("programName") : "-");
            map.put("Ngày bắt đầu", r.get("startDate"));
            map.put("Ngày kết thúc", r.get("endDate"));
            formattedRows.add(map);
        }

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
